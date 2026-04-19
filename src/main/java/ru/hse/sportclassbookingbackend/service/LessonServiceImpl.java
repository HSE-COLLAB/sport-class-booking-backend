package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonPatchRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonResponse;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonStatus;
import ru.hse.sportclassbookingbackend.dto.lesson.RecurringLessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.RecurringLessonResponse;
import ru.hse.sportclassbookingbackend.exception.BadRequestException;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.LessonMapper;
import ru.hse.sportclassbookingbackend.model.Campus;
import ru.hse.sportclassbookingbackend.model.Lesson;
import ru.hse.sportclassbookingbackend.model.Teacher;
import ru.hse.sportclassbookingbackend.model.WorkoutType;
import ru.hse.sportclassbookingbackend.repository.CampusRepository;
import ru.hse.sportclassbookingbackend.repository.LessonRepository;
import ru.hse.sportclassbookingbackend.repository.TeacherRepository;
import ru.hse.sportclassbookingbackend.repository.WorkoutTypeRepository;
import ru.hse.sportclassbookingbackend.security.UserPrincipal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private static final int MAX_RECURRING_DAYS = 365;

    private final LessonRepository lessonRepository;
    private final WorkoutTypeRepository workoutTypeRepository;
    private final TeacherRepository teacherRepository;
    private final CampusRepository campusRepository;
    private final LessonMapper lessonMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<LessonResponse> getAll(UUID workoutTypeId, UUID teacherId, Integer campusId, OffsetDateTime from,
                                       OffsetDateTime to, String place, LessonStatus status, Pageable pageable) {
        Sort sort = resolveSort(status);
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        String statusStr = status != null ? status.name() : null;

        return lessonRepository.findAllWithFilters(
                workoutTypeId, teacherId, campusId, from, to, place, statusStr, OffsetDateTime.now(), sortedPageable
        ).map(lessonMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonResponse getById(UUID id) {
        return lessonMapper.toResponse(findLessonOrThrow(id));
    }

    @Override
    @Transactional
    public LessonResponse create(LessonRequest request, UserPrincipal principal) {
        validateTimeRange(request.startTime(), request.endTime());

        Teacher teacher = findTeacherOrThrow(principal.getId());
        WorkoutType workoutType = findActiveWorkoutTypeOrThrow(request.workoutTypeId());
        Campus campus = findCampusOrThrow(request.campusId());

        Lesson lesson = lessonMapper.toEntity(request);
        lesson.setStartTime(toCampusOffsetDateTime(request.startTime(), campus));
        lesson.setEndTime(toCampusOffsetDateTime(request.endTime(), campus));
        lesson.setTeacher(teacher);
        lesson.setCampus(campus);
        lesson.setWorkoutType(workoutType);

        return lessonMapper.toResponse(lessonRepository.save(lesson));
    }

    @Override
    @Transactional
    public RecurringLessonResponse createRecurring(RecurringLessonRequest request, UserPrincipal principal) {
        validateRecurringRequest(request);

        Teacher teacher = findTeacherOrThrow(principal.getId());
        WorkoutType workoutType = findActiveWorkoutTypeOrThrow(request.workoutTypeId());
        Campus campus = findCampusOrThrow(request.campusId());

        List<Lesson> lessons = generateLessons(request, teacher, workoutType, campus);

        if (lessons.isEmpty()) {
            throw new BadRequestException("No lessons could be created for the given schedule");
        }

        lessonRepository.saveAll(lessons);

        LocalDate firstDate = lessons.getFirst().getStartTime().toLocalDate();
        LocalDate lastDate = lessons.getLast().getStartTime().toLocalDate();

        return new RecurringLessonResponse(lessons.size(), firstDate, lastDate);
    }

    @Override
    @Transactional
    public LessonResponse update(UUID id, LessonPatchRequest request, UserPrincipal principal) {
        Lesson lesson = findLessonOrThrow(id);
        checkOwnership(lesson, principal);

        lessonMapper.updateFromPatch(request, lesson);

        if (request.workoutTypeId() != null) {
            lesson.setWorkoutType(findActiveWorkoutTypeOrThrow(request.workoutTypeId()));
        }
        if (request.campusId() != null) {
            lesson.setCampus(findCampusOrThrow(request.campusId()));
        }
        if (request.startTime() != null) {
            lesson.setStartTime(toCampusOffsetDateTime(request.startTime(), lesson.getCampus()));
        }
        if (request.endTime() != null) {
            lesson.setEndTime(toCampusOffsetDateTime(request.endTime(), lesson.getCampus()));
        }
        if (request.startTime() != null || request.endTime() != null) {
            validateTimeRange(lesson.getStartTime(), lesson.getEndTime());
        }

        return lessonMapper.toResponse(lessonRepository.save(lesson));
    }

    @Override
    @Transactional
    public void delete(UUID id, UserPrincipal principal) {
        lessonRepository.findById(id).ifPresent(lesson -> {
            checkOwnership(lesson, principal);
            lessonRepository.delete(lesson);
        });
    }

    private void validateRecurringRequest(RecurringLessonRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("endTime must be after startTime");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("endDate must not be before startDate");
        }
        if (request.startDate().plusDays(MAX_RECURRING_DAYS).isBefore(request.endDate())) {
            throw new BadRequestException("endDate must not be more than " + MAX_RECURRING_DAYS + " days after startDate");
        }
    }

    private List<Lesson> generateLessons(RecurringLessonRequest request, Teacher teacher,
                                         WorkoutType workoutType, Campus campus) {
        List<Lesson> lessons = new ArrayList<>();
        LocalDate current = request.startDate();
        ZoneId zoneId = ZoneId.of(campus.getTimezone());

        while (!current.isAfter(request.endDate())) {
            if (request.daysOfWeek().contains(current.getDayOfWeek())) {
                ZoneOffset offset = zoneId.getRules().getOffset(current.atTime(request.startTime()));
                OffsetDateTime startDateTime = current.atTime(request.startTime()).atOffset(offset);
                OffsetDateTime endDateTime = current.atTime(request.endTime()).atOffset(offset);

                if (startDateTime.isAfter(OffsetDateTime.now())) {
                    Lesson lesson = lessonMapper.toEntityFromRecurring(request, startDateTime, endDateTime);
                    lesson.setTeacher(teacher);
                    lesson.setCampus(campus);
                    lesson.setWorkoutType(workoutType);
                    lessons.add(lesson);
                }
            }
            current = current.plusDays(1);
        }

        return lessons;
    }

    private OffsetDateTime toCampusOffsetDateTime(LocalDateTime localDateTime, Campus campus) {
        ZoneId zoneId = ZoneId.of(campus.getTimezone());
        ZoneOffset offset = zoneId.getRules().getOffset(localDateTime);
        return localDateTime.atOffset(offset);
    }

    private void checkOwnership(Lesson lesson, UserPrincipal principal) {
        if (!lesson.getTeacher().getId().equals(principal.getId())) {
            throw new BadRequestException("You can only modify your own lessons");
        }
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("endTime must be after startTime");
        }
    }

    private void validateTimeRange(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("endTime must be after startTime");
        }
    }

    private Lesson findLessonOrThrow(UUID id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lesson with id: " + id + " was not found"));
    }

    private WorkoutType findActiveWorkoutTypeOrThrow(UUID id) {
        return workoutTypeRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new BadRequestException("Active workout type with id: " + id + " was not found"));
    }

    private Teacher findTeacherOrThrow(UUID id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Teacher with id: " + id + " was not found"));
    }

    private Campus findCampusOrThrow(Integer id) {
        return campusRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Campus with id: " + id + " was not found"));
    }

    private Sort resolveSort(LessonStatus status) {
        if (status == LessonStatus.PAST) {
            return Sort.by(Sort.Direction.DESC, "startTime");
        }
        return Sort.by(Sort.Direction.ASC, "startTime");
    }
}
