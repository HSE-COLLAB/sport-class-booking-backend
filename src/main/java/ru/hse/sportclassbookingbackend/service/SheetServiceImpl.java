package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonStatus;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonTimeStatus;
import ru.hse.sportclassbookingbackend.dto.sheet.AttendanceMark;
import ru.hse.sportclassbookingbackend.dto.sheet.AttendeeResponse;
import ru.hse.sportclassbookingbackend.dto.sheet.BulkAttendanceRequest;
import ru.hse.sportclassbookingbackend.dto.sheet.MyLessonResponse;
import ru.hse.sportclassbookingbackend.dto.sheet.SheetIdResponse;
import ru.hse.sportclassbookingbackend.exception.BadRequestException;
import ru.hse.sportclassbookingbackend.exception.ConflictException;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.LessonMapper;
import ru.hse.sportclassbookingbackend.mapper.SheetMapper;
import ru.hse.sportclassbookingbackend.model.Lesson;
import ru.hse.sportclassbookingbackend.model.Role;
import ru.hse.sportclassbookingbackend.model.Sheet;
import ru.hse.sportclassbookingbackend.model.Student;
import ru.hse.sportclassbookingbackend.repository.LessonRepository;
import ru.hse.sportclassbookingbackend.repository.SheetRepository;
import ru.hse.sportclassbookingbackend.repository.StudentRepository;
import ru.hse.sportclassbookingbackend.security.UserPrincipal;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SheetServiceImpl implements SheetService {

    private final SheetRepository sheetRepository;
    private final LessonRepository lessonRepository;
    private final StudentRepository studentRepository;
    private final SheetMapper sheetMapper;
    private final LessonMapper lessonMapper;

    @Override
    @Transactional
    public SheetIdResponse register(UUID lessonId, UserPrincipal principal) {
        Lesson lesson = lessonRepository.findByIdForUpdate(lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson with id: " + lessonId + " was not found"));
        Student student = findStudentOrThrow(principal.getId());

        if (lesson.getStatus() == LessonStatus.CANCELLED) {
            throw new ConflictException("Cannot register on a cancelled lesson");
        }
        if (!lesson.getStartTime().isAfter(OffsetDateTime.now())) {
            throw new BadRequestException("Cannot register on a lesson that has already started");
        }
        if (!student.getCampus().getId().equals(lesson.getCampus().getId())) {
            throw new BadRequestException("You can only register on lessons in your campus");
        }
        if (!lesson.getWorkoutType().getAllowedHealthGroups().contains(student.getHealthGroup())) {
            throw new BadRequestException("This workout type is not allowed for your health group");
        }
        if (sheetRepository.existsByLessonIdAndStudentId(lessonId, student.getId())) {
            throw new ConflictException("You are already registered on this lesson");
        }
        if (sheetRepository.countByLessonId(lessonId) >= lesson.getTotalPlaces()) {
            throw new ConflictException("No free places on this lesson");
        }
        if (sheetRepository.hasTimeOverlap(student.getId(), lesson.getStartTime(), lesson.getEndTime())) {
            throw new ConflictException("You have another lesson at this time");
        }

        Sheet sheet = new Sheet();
        sheet.setLesson(lesson);
        sheet.setStudent(student);
        sheet.setVisited(false);

        return new SheetIdResponse(sheetRepository.save(sheet).getId());
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
        if (!sheet.getLesson().getStartTime().isAfter(OffsetDateTime.now())) {
            throw new BadRequestException("Cannot cancel registration after the lesson has started");
        }

        checkCancelPermission(sheet, principal);

        sheetRepository.delete(sheet);
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
            throw new BadRequestException("You can only mark attendance on your own lessons");
        }
        if (lesson.getStatus() == LessonStatus.CANCELLED) {
            throw new ConflictException("Cannot mark attendance on a cancelled lesson");
        }
        if (lesson.getStartTime().isAfter(OffsetDateTime.now())) {
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

        return sheets.stream()
                .map(sheetMapper::toAttendeeResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MyLessonResponse> getMyLessons(Collection<LessonTimeStatus> timeStatuses, Boolean visited,
                                               LocalDateTime from, LocalDateTime to, Pageable pageable,
                                               UserPrincipal principal) {
        Student student = findStudentOrThrow(principal.getId());
        ZoneId zoneId = ZoneId.of(student.getCampus().getTimezone());

        if (from != null && to != null && !to.isAfter(from)) {
            throw new BadRequestException("'to' must be after 'from'");
        }

        Collection<LessonTimeStatus> effectiveStatuses = timeStatuses;
        if (effectiveStatuses == null || effectiveStatuses.isEmpty()) {
            effectiveStatuses = List.of(LessonTimeStatus.UPCOMING, LessonTimeStatus.ONGOING);
        }
        Collection<String> statusStrings = effectiveStatuses.stream().map(Enum::name).toList();

        Sort sort = resolveSort(effectiveStatuses);
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        OffsetDateTime fromUtc = from != null ? from.atZone(zoneId).toOffsetDateTime() : null;
        OffsetDateTime toUtc = to != null ? to.atZone(zoneId).toOffsetDateTime() : null;

        return sheetRepository.findAllMyLessons(
                student.getId(), fromUtc, toUtc, visited, statusStrings, OffsetDateTime.now(), sortedPageable
        ).map(this::toMyLessonResponse);
    }

    private MyLessonResponse toMyLessonResponse(Sheet sheet) {
        Lesson lesson = sheet.getLesson();
        int taken = (int) sheetRepository.countByLessonId(lesson.getId());
        int available = Math.max(0, lesson.getTotalPlaces() - taken);
        ZoneId zoneId = ZoneId.of(lesson.getCampus().getTimezone());

        return new MyLessonResponse(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getPlace(),
                lesson.getStartTime().atZoneSameInstant(zoneId).toOffsetDateTime(),
                lesson.getEndTime().atZoneSameInstant(zoneId).toOffsetDateTime(),
                lesson.getTotalPlaces(),
                available,
                lesson.getStatus(),
                lesson.getNotes(),
                lessonMapper.toWorkoutTypeResponse(lesson.getWorkoutType()),
                lessonMapper.toTeacherShortResponse(lesson.getTeacher()),
                lessonMapper.toCampusResponse(lesson.getCampus()),
                new MyLessonResponse.MySheetInfo(sheet.getId(), sheet.getVisited())
        );
    }

    private void checkCancelPermission(Sheet sheet, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMIN) {
            return;
        }
        if (principal.getRole() == Role.STUDENT) {
            if (!sheet.getStudent().getId().equals(principal.getId())) {
                throw new BadRequestException("You can only cancel your own registration");
            }
            return;
        }
        if (principal.getRole() == Role.TEACHER) {
            if (!sheet.getLesson().getTeacher().getId().equals(principal.getId())) {
                throw new BadRequestException("You can only cancel registrations on your own lessons");
            }
            return;
        }
        throw new BadRequestException("Access denied");
    }

    private Lesson findLessonOrThrow(UUID id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lesson with id: " + id + " was not found"));
    }

    private Student findStudentOrThrow(UUID id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Student with id: " + id + " was not found"));
    }

    private Sort resolveSort(Collection<LessonTimeStatus> statuses) {
        boolean onlyPast = statuses != null
                && statuses.size() == 1
                && statuses.iterator().next() == LessonTimeStatus.PAST;
        if (onlyPast) {
            return Sort.by(Sort.Direction.DESC, "lesson.startTime");
        }
        return Sort.by(Sort.Direction.ASC, "lesson.startTime");
    }
}
