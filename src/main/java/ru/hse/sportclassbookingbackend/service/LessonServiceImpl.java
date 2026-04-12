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
import ru.hse.sportclassbookingbackend.model.Lesson;
import ru.hse.sportclassbookingbackend.model.Role;
import ru.hse.sportclassbookingbackend.model.Teacher;
import ru.hse.sportclassbookingbackend.model.WorkoutType;
import ru.hse.sportclassbookingbackend.repository.LessonRepository;
import ru.hse.sportclassbookingbackend.repository.TeacherRepository;
import ru.hse.sportclassbookingbackend.repository.WorkoutTypeRepository;
import ru.hse.sportclassbookingbackend.security.UserPrincipal;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
    private final LessonMapper lessonMapper;

    @Override
    public Page<LessonResponse> getAll(UUID workoutTypeId, UUID teacherId, OffsetDateTime from, OffsetDateTime to,
                                       String place, LessonStatus status, Pageable pageable) {
        Sort sort = resolveSort(status);
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        String statusStr = status != null ? status.name() : null;

        return lessonRepository.findAllWithFilters(
                workoutTypeId, teacherId, from, to, place, statusStr, OffsetDateTime.now(), sortedPageable
        ).map(lessonMapper::toResponse);
    }

    @Override
    public LessonResponse getById(UUID id) {
        Lesson lesson = findLessonOrThrow(id);
        return lessonMapper.toResponse(lesson);
    }

    @Override
    @Transactional
    public LessonResponse create(LessonRequest request, UserPrincipal principal) {
        validateTimeRange(request.startTime(), request.endTime());

        WorkoutType workoutType = findActiveWorkoutTypeOrThrow(request.workoutTypeId());
        Teacher teacher = resolveTeacher(request.teacherId(), principal);

        Lesson lesson = lessonMapper.toEntity(request);
        lesson.setWorkoutType(workoutType);
        lesson.setTeacher(teacher);

        return lessonMapper.toResponse(lessonRepository.save(lesson));
    }

    @Override
    @Transactional
    public RecurringLessonResponse createRecurring(RecurringLessonRequest request, UserPrincipal principal) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("endTime must be after startTime");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("endDate must not be before startDate");
        }
        if (request.startDate().plusDays(MAX_RECURRING_DAYS).isBefore(request.endDate())) {
            throw new BadRequestException("endDate must not be more than " + MAX_RECURRING_DAYS + " days after startDate");
        }

        WorkoutType workoutType = findActiveWorkoutTypeOrThrow(request.workoutTypeId());
        Teacher teacher = resolveTeacher(request.teacherId(), principal);

        List<Lesson> lessons = new ArrayList<>();
        LocalDate current = request.startDate();

        while (!current.isAfter(request.endDate())) {
            if (request.daysOfWeek().contains(current.getDayOfWeek())) {
                OffsetDateTime startDateTime = current.atTime(request.startTime()).atOffset(ZoneOffset.UTC);
                OffsetDateTime endDateTime = current.atTime(request.endTime()).atOffset(ZoneOffset.UTC);

                if (startDateTime.isAfter(OffsetDateTime.now())) {
                    Lesson lesson = new Lesson();
                    lesson.setTitle(request.title());
                    lesson.setPlace(request.place());
                    lesson.setStartTime(startDateTime);
                    lesson.setEndTime(endDateTime);
                    lesson.setTotalPlaces(request.totalPlaces());
                    lesson.setNotes(request.notes());
                    lesson.setWorkoutType(workoutType);
                    lesson.setTeacher(teacher);
                    lessons.add(lesson);
                }
            }
            current = current.plusDays(1);
        }

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

        if (request.title() != null) {
            lesson.setTitle(request.title());
        }
        if (request.place() != null) {
            lesson.setPlace(request.place());
        }
        if (request.startTime() != null) {
            lesson.setStartTime(request.startTime());
        }
        if (request.endTime() != null) {
            lesson.setEndTime(request.endTime());
        }
        if (request.startTime() != null || request.endTime() != null) {
            validateTimeRange(lesson.getStartTime(), lesson.getEndTime());
        }
        if (request.totalPlaces() != null) {
            lesson.setTotalPlaces(request.totalPlaces());
        }
        if (request.workoutTypeId() != null) {
            WorkoutType workoutType = findActiveWorkoutTypeOrThrow(request.workoutTypeId());
            lesson.setWorkoutType(workoutType);
        }
        if (request.teacherId() != null) {
            Teacher teacher = findTeacherOrThrow(request.teacherId());
            lesson.setTeacher(teacher);
        }
        if (request.notes() != null) {
            lesson.setNotes(request.notes());
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

    private Teacher resolveTeacher(UUID teacherIdFromRequest, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMIN) {
            if (teacherIdFromRequest == null) {
                throw new BadRequestException("teacherId is required for admin");
            }
            return findTeacherOrThrow(teacherIdFromRequest);
        }
        return findTeacherOrThrow(principal.getId());
    }

    private void checkOwnership(Lesson lesson, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMIN) {
            return;
        }
        if (!lesson.getTeacher().getId().equals(principal.getId())) {
            throw new BadRequestException("You can only modify your own lessons");
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

    private Sort resolveSort(LessonStatus status) {
        if (status == LessonStatus.PAST) {
            return Sort.by(Sort.Direction.DESC, "startTime");
        }
        return Sort.by(Sort.Direction.ASC, "startTime");
    }
}
