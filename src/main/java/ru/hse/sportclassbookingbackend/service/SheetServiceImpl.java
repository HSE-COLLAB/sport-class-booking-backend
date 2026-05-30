package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonStatus;
import ru.hse.sportclassbookingbackend.dto.sheet.AttendanceMark;
import ru.hse.sportclassbookingbackend.event.SheetCancelledByOtherEvent;
import ru.hse.sportclassbookingbackend.event.SheetCreatedEvent;
import ru.hse.sportclassbookingbackend.dto.sheet.AttendeeResponse;
import ru.hse.sportclassbookingbackend.dto.sheet.BulkAttendanceRequest;
import ru.hse.sportclassbookingbackend.dto.sheet.SheetIdResponse;
import ru.hse.sportclassbookingbackend.exception.BadRequestException;
import ru.hse.sportclassbookingbackend.exception.ConflictException;
import ru.hse.sportclassbookingbackend.exception.ForbiddenException;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.SheetMapper;
import ru.hse.sportclassbookingbackend.model.Lesson;
import ru.hse.sportclassbookingbackend.model.Role;
import ru.hse.sportclassbookingbackend.model.Sheet;
import ru.hse.sportclassbookingbackend.model.Student;
import ru.hse.sportclassbookingbackend.repository.LessonRepository;
import ru.hse.sportclassbookingbackend.repository.SheetRepository;
import ru.hse.sportclassbookingbackend.repository.StudentRepository;
import ru.hse.sportclassbookingbackend.security.UserPrincipal;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SheetServiceImpl implements SheetService {

    private final SheetRepository sheetRepository;
    private final LessonRepository lessonRepository;
    private final StudentRepository studentRepository;
    private final SheetMapper sheetMapper;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SheetIdResponse register(UUID lessonId, UserPrincipal principal) {
        Lesson lesson = lessonRepository.findByIdForUpdate(lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson with id: " + lessonId + " was not found"));
        Student student = findStudentOrThrow(principal.getId());

        if (lesson.getStatus() == LessonStatus.CANCELLED) {
            throw new ConflictException("Cannot register on a cancelled lesson");
        }
        if (!lesson.getStartTime().isAfter(OffsetDateTime.now(clock))) {
            throw new BadRequestException("Cannot register on a lesson that has already started");
        }
        if (!student.getCampus().getId().equals(lesson.getCampus().getId())) {
            throw new ForbiddenException("Cannot register on lessons outside your campus");
        }
        if (!lesson.getWorkoutType().getAllowedHealthGroups().contains(student.getHealthGroup())) {
            throw new BadRequestException("Workout type is not allowed for current health group");
        }
        if (sheetRepository.existsByLessonIdAndStudentId(lessonId, student.getId())) {
            throw new ConflictException("Already registered on this lesson");
        }
        if (sheetRepository.countByLessonId(lessonId) >= lesson.getTotalPlaces()) {
            throw new ConflictException("Lesson has no available places");
        }
        if (sheetRepository.hasTimeOverlap(student.getId(), lesson.getStartTime(), lesson.getEndTime())) {
            throw new ConflictException("Another lesson is already scheduled at this time");
        }

        Sheet sheet = new Sheet();
        sheet.setLesson(lesson);
        sheet.setStudent(student);
        sheet.setVisited(false);
        Sheet saved = sheetRepository.save(sheet);

        eventPublisher.publishEvent(new SheetCreatedEvent(saved.getId(), lesson.getId(), student.getId()));
        log.info("Sheet registered: sheetId={} lessonId={} studentId={} (confirmation email queued)",
                saved.getId(), lesson.getId(), student.getId());

        return new SheetIdResponse(saved.getId());
    }

    @Override
    @Transactional
    public void cancel(UUID lessonId, UUID sheetId, UserPrincipal principal) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new NotFoundException("Sheet with id: " + sheetId + " was not found"));

        if (!sheet.getLesson().getId().equals(lessonId)) {
            throw new BadRequestException("Sheet does not belong to the specified lesson");
        }
        if (sheet.getLesson().getStatus() == LessonStatus.CANCELLED) {
            throw new ConflictException("Cannot cancel registration on a cancelled lesson");
        }
        if (!sheet.getLesson().getStartTime().isAfter(OffsetDateTime.now(clock))) {
            throw new BadRequestException("Cannot cancel registration after the lesson has started");
        }

        checkCancelPermission(sheet, principal);

        boolean cancelledByOther = !sheet.getStudent().getId().equals(principal.getId());

        sheetRepository.delete(sheet);
        log.info("Sheet cancelled: sheetId={} lessonId={} studentId={} by userId={} role={}",
                sheetId, lessonId, sheet.getStudent().getId(), principal.getId(), principal.getRole());

        if (cancelledByOther) {
            eventPublisher.publishEvent(new SheetCancelledByOtherEvent(
                    sheet.getId(),
                    sheet.getLesson().getId(),
                    sheet.getStudent().getId(),
                    principal.getId()
            ));
            log.info("SheetCancelledByOtherEvent published: sheetId={} (notification email queued)", sheetId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendeeResponse> getAttendees(UUID lessonId) {
        findLessonOrThrow(lessonId);
        return sheetRepository.findAllByLessonIdWithStudent(lessonId).stream()
                .map(sheetMapper::toAttendeeResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<AttendeeResponse> markAttendance(UUID lessonId, BulkAttendanceRequest request, UserPrincipal principal) {
        Lesson lesson = findLessonOrThrow(lessonId);

        if (principal.getRole() == Role.TEACHER && !lesson.getTeacher().getId().equals(principal.getId())) {
            throw new ForbiddenException("Cannot mark attendance on another teacher's lessons");
        }
        if (lesson.getStatus() == LessonStatus.CANCELLED) {
            throw new ConflictException("Cannot mark attendance on a cancelled lesson");
        }
        if (lesson.getStartTime().isAfter(OffsetDateTime.now(clock))) {
            throw new BadRequestException("Cannot mark attendance before the lesson starts");
        }

        List<Sheet> sheets = sheetRepository.findAllByLessonIdWithStudent(lessonId);
        Map<UUID, Sheet> sheetsById = sheets.stream()
                .collect(Collectors.toMap(Sheet::getId, s -> s));

        for (AttendanceMark mark : request.marks()) {
            Sheet sheet = sheetsById.get(mark.sheetId());
            if (sheet == null) {
                throw new BadRequestException("Sheet with id " + mark.sheetId() + " does not belong to this lesson");
            }
            sheet.setVisited(mark.visited());
        }

        sheetRepository.saveAll(sheets);

        log.info("Attendance marked: lessonId={} sheets={} by userId={} role={}",
                lessonId, request.marks().size(), principal.getId(), principal.getRole());
        return sheets.stream()
                .map(sheetMapper::toAttendeeResponse)
                .toList();
    }

    private void checkCancelPermission(Sheet sheet, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMIN) {
            return;
        }
        if (principal.getRole() == Role.STUDENT) {
            if (!sheet.getStudent().getId().equals(principal.getId())) {
                throw new ForbiddenException("Cannot cancel another student's registration");
            }
            return;
        }
        if (principal.getRole() == Role.TEACHER) {
            if (!sheet.getLesson().getTeacher().getId().equals(principal.getId())) {
                throw new ForbiddenException("Cannot cancel registrations on another teacher's lessons");
            }
            return;
        }
        throw new ForbiddenException("Access denied");
    }

    private Lesson findLessonOrThrow(UUID id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lesson with id: " + id + " was not found"));
    }

    private Student findStudentOrThrow(UUID id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student with id: " + id + " was not found"));
    }
}
