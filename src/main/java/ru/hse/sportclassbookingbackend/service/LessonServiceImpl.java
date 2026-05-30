package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import ru.hse.sportclassbookingbackend.dto.lesson.LessonTimeStatus;
import ru.hse.sportclassbookingbackend.dto.lesson.RecurringLessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.RecurringLessonResponse;
import ru.hse.sportclassbookingbackend.dto.sheet.MyLessonResponse;
import ru.hse.sportclassbookingbackend.exception.BadRequestException;
import ru.hse.sportclassbookingbackend.exception.ConflictException;
import ru.hse.sportclassbookingbackend.exception.ForbiddenException;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.LessonMapper;
import ru.hse.sportclassbookingbackend.model.Campus;
import ru.hse.sportclassbookingbackend.model.Lesson;
import ru.hse.sportclassbookingbackend.model.Role;
import ru.hse.sportclassbookingbackend.model.Sheet;
import ru.hse.sportclassbookingbackend.model.Student;
import ru.hse.sportclassbookingbackend.model.Teacher;
import ru.hse.sportclassbookingbackend.model.WorkoutType;
import ru.hse.sportclassbookingbackend.repository.CampusRepository;
import ru.hse.sportclassbookingbackend.repository.LessonRepository;
import ru.hse.sportclassbookingbackend.repository.SheetRepository;
import ru.hse.sportclassbookingbackend.repository.StudentRepository;
import ru.hse.sportclassbookingbackend.repository.TeacherRepository;
import ru.hse.sportclassbookingbackend.repository.WorkoutTypeRepository;
import ru.hse.sportclassbookingbackend.security.UserPrincipal;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private static final int MAX_RECURRING_DAYS = 365;

    private final LessonRepository lessonRepository;
    private final WorkoutTypeRepository workoutTypeRepository;
    private final TeacherRepository teacherRepository;
    private final CampusRepository campusRepository;
    private final StudentRepository studentRepository;
    private final SheetRepository sheetRepository;
    private final LessonMapper lessonMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Page<LessonResponse> getAll(Integer campusId, Collection<UUID> workoutTypeIds, UUID teacherId,
                                       LocalDateTime from, LocalDateTime to, String place,
                                       Boolean myHealthGroup, Collection<LessonTimeStatus> timeStatuses,
                                       Boolean includeCancelled, Pageable pageable, UserPrincipal principal) {
        Campus campus = findCampusOrThrow(campusId);

        if (from != null && to != null && !to.isAfter(from)) {
            throw new BadRequestException("'to' must be after 'from'");
        }

        Collection<LessonTimeStatus> effectiveStatuses = timeStatuses;
        if (effectiveStatuses == null || effectiveStatuses.isEmpty()) {
            effectiveStatuses = List.of(LessonTimeStatus.UPCOMING, LessonTimeStatus.ONGOING, LessonTimeStatus.PAST);
        }
        Collection<String> statusStrings = effectiveStatuses.stream().map(Enum::name).toList();

        Collection<UUID> effectiveWorkoutTypeIds = (workoutTypeIds == null || workoutTypeIds.isEmpty())
                ? null
                : workoutTypeIds;

        Sort sort = resolveSort(effectiveStatuses);
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        OffsetDateTime fromUtc = from != null ? toCampusOffsetDateTime(from, campus) : null;
        OffsetDateTime toUtc = to != null ? toCampusOffsetDateTime(to, campus) : null;

        Integer healthGroupId = null;
        if (Boolean.TRUE.equals(myHealthGroup)) {
            if (principal.getRole() != Role.STUDENT) {
                throw new ForbiddenException("Filter 'myHealthGroup' is only available to students");
            }
            Student student = findStudentOrThrow(principal.getId());
            healthGroupId = student.getHealthGroup().getId();
        }

        return lessonRepository.findAllWithFilters(
                campusId, effectiveWorkoutTypeIds, teacherId, fromUtc, toUtc, place, healthGroupId,
                statusStrings, !Boolean.FALSE.equals(includeCancelled), OffsetDateTime.now(clock), sortedPageable
        ).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonResponse getById(UUID id) {
        return toResponse(findLessonOrThrow(id));
    }

    @Override
    @Transactional
    public LessonResponse create(LessonRequest request, UserPrincipal principal) {
        validateTimeRange(request.startTime(), request.endTime());

        Campus campus = findCampusOrThrow(request.campusId());
        WorkoutType workoutType = findActiveWorkoutTypeOrThrow(request.workoutTypeId());
        Teacher teacher = resolveTeacher(request.teacherId(), campus, principal);

        OffsetDateTime startTime = toCampusOffsetDateTime(request.startTime(), campus);
        OffsetDateTime endTime = toCampusOffsetDateTime(request.endTime(), campus);

        if (lessonRepository.hasTeacherTimeOverlap(teacher.getId(), startTime, endTime, null)) {
            throw new ConflictException("Teacher already has a lesson at this time");
        }

        Lesson lesson = lessonMapper.toEntity(request);
        lesson.setStartTime(startTime);
        lesson.setEndTime(endTime);
        lesson.setTeacher(teacher);
        lesson.setCampus(campus);
        lesson.setWorkoutType(workoutType);
        lesson.setStatus(LessonStatus.ACTIVE);

        Lesson saved = lessonRepository.save(lesson);
        log.info("Lesson created: lessonId={} teacherId={} campusId={} startTime={}",
                saved.getId(), teacher.getId(), campus.getId(), saved.getStartTime());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public RecurringLessonResponse createRecurring(RecurringLessonRequest request, UserPrincipal principal) {
        validateRecurringRequest(request);

        Campus campus = findCampusOrThrow(request.campusId());
        WorkoutType workoutType = findActiveWorkoutTypeOrThrow(request.workoutTypeId());
        Teacher teacher = resolveTeacher(request.teacherId(), campus, principal);

        List<Lesson> lessons = generateLessons(request, teacher, workoutType, campus);

        if (lessons.isEmpty()) {
            throw new BadRequestException("No lessons could be created for the given schedule");
        }

        for (Lesson lesson : lessons) {
            if (lessonRepository.hasTeacherTimeOverlap(teacher.getId(), lesson.getStartTime(), lesson.getEndTime(), null)) {
                throw new ConflictException("Teacher already has a lesson at " + lesson.getStartTime());
            }
        }

        lessonRepository.saveAll(lessons);

        LocalDate firstDate = lessons.getFirst().getStartTime().toLocalDate();
        LocalDate lastDate = lessons.getLast().getStartTime().toLocalDate();

        log.info("Recurring lessons created: count={} teacherId={} firstDate={} lastDate={}",
                lessons.size(), teacher.getId(), firstDate, lastDate);
        return new RecurringLessonResponse(lessons.size(), firstDate, lastDate);
    }

    @Override
    @Transactional
    public LessonResponse update(UUID id, LessonPatchRequest request, UserPrincipal principal) {
        Lesson lesson = findLessonOrThrow(id);
        checkEditPermission(lesson, principal);

        if (lesson.getStatus() == LessonStatus.CANCELLED) {
            throw new ConflictException("Cannot edit cancelled lesson");
        }

        lessonMapper.updateFromPatch(request, lesson);

        if (request.workoutTypeId() != null) {
            lesson.setWorkoutType(findActiveWorkoutTypeOrThrow(request.workoutTypeId()));
        }
        if (request.campusId() != null) {
            Campus newCampus = findCampusOrThrow(request.campusId());
            if (principal.getRole() == Role.TEACHER) {
                Teacher teacher = findTeacherOrThrow(principal.getId());
                if (!newCampus.getId().equals(teacher.getCampus().getId())) {
                    throw new ForbiddenException("Can only modify lessons in your own campus");
                }
            }
            lesson.setCampus(newCampus);
        }
        if (request.teacherId() != null) {
            if (principal.getRole() == Role.TEACHER) {
                throw new ForbiddenException("Cannot reassign lesson to another teacher");
            }
            Teacher newTeacher = findTeacherOrThrow(request.teacherId());
            if (!newTeacher.getCampus().getId().equals(lesson.getCampus().getId())) {
                throw new BadRequestException("Teacher must belong to the same campus as the lesson");
            }
            lesson.setTeacher(newTeacher);
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
        if (request.startTime() != null || request.endTime() != null || request.teacherId() != null) {
            if (lessonRepository.hasTeacherTimeOverlap(lesson.getTeacher().getId(),
                    lesson.getStartTime(), lesson.getEndTime(), lesson.getId())) {
                throw new ConflictException("Teacher already has a lesson at this time");
            }
        }
        if (request.totalPlaces() != null) {
            long taken = sheetRepository.countByLessonId(lesson.getId());
            if (taken > request.totalPlaces()) {
                throw new ConflictException(
                        "Cannot set totalPlaces to " + request.totalPlaces()
                                + ": " + taken + " students are already registered");
            }
        }

        Lesson saved = lessonRepository.save(lesson);
        log.info("Lesson updated: lessonId={} by userId={} role={}",
                saved.getId(), principal.getId(), principal.getRole());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public LessonResponse cancel(UUID id, UserPrincipal principal) {
        Lesson lesson = findLessonOrThrow(id);
        checkEditPermission(lesson, principal);

        if (lesson.getStatus() == LessonStatus.CANCELLED) {
            throw new ConflictException("Lesson is already cancelled");
        }
        if (!lesson.getEndTime().isAfter(OffsetDateTime.now(clock))) {
            throw new ConflictException("Cannot cancel a lesson that has already ended");
        }

        lesson.setStatus(LessonStatus.CANCELLED);
        Lesson saved = lessonRepository.save(lesson);
        log.info("Lesson cancelled: lessonId={} by userId={} role={}",
                saved.getId(), principal.getId(), principal.getRole());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MyLessonResponse> getMyLessons(Collection<LessonTimeStatus> timeStatuses, Boolean visited,
                                               LocalDateTime from, LocalDateTime to, Pageable pageable,
                                               UserPrincipal principal) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new BadRequestException("'to' must be after 'from'");
        }

        Collection<LessonTimeStatus> effectiveStatuses = timeStatuses;
        if (effectiveStatuses == null || effectiveStatuses.isEmpty()) {
            effectiveStatuses = List.of(LessonTimeStatus.UPCOMING, LessonTimeStatus.ONGOING, LessonTimeStatus.PAST);
        }
        Collection<String> statusStrings = effectiveStatuses.stream().map(Enum::name).toList();

        Sort sort = resolveSort(effectiveStatuses);


        if (principal.getRole() == Role.STUDENT) {
            Sort adjustedSort = Sort.by(
                    sort.stream()
                            .map(order -> new Sort.Order(
                                    order.getDirection(),
                                    "lesson." + order.getProperty()
                            ))
                            .toList()
            );

            Pageable sortedPageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    adjustedSort
            );
            Student student = findStudentOrThrow(principal.getId());
            ZoneId zoneId = ZoneId.of(student.getCampus().getTimezone());


            OffsetDateTime fromUtc = from != null ? from.atZone(zoneId).toOffsetDateTime() : null;
            OffsetDateTime toUtc = to != null ? to.atZone(zoneId).toOffsetDateTime() : null;

            return sheetRepository.findAllMyLessons(
                    student.getId(), fromUtc, toUtc, visited, statusStrings, OffsetDateTime.now(), sortedPageable
            ).map(this::toMyLessonResponse);
        }
        else {
            if (visited != null) {
                throw new BadRequestException("Parameter 'visited' is only available for STUDENT role");
            }

            Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            Teacher teacher = findTeacherOrThrow(principal.getId());
            ZoneId zoneId = ZoneId.of(teacher.getCampus().getTimezone());

            OffsetDateTime fromUtc = from != null ? from.atZone(zoneId).toOffsetDateTime() : null;
            OffsetDateTime toUtc = to != null ? to.atZone(zoneId).toOffsetDateTime() : null;

            return lessonRepository.findTeacherLessons(
                            teacher.getId(),
                            fromUtc,
                            toUtc,
                            statusStrings,
                            OffsetDateTime.now(),
                            sortedPageable
                    ).map(this::toMyLessonResponseFromLesson);
        }
    }




    private Teacher resolveTeacher(UUID teacherIdFromRequest, Campus campus, UserPrincipal principal) {
        if (principal.getRole() == Role.TEACHER) {
            if (teacherIdFromRequest != null && !teacherIdFromRequest.equals(principal.getId())) {
                throw new ForbiddenException("Cannot assign lesson to another teacher");
            }
            Teacher teacher = findTeacherOrThrow(principal.getId());
            if (!teacher.getCampus().getId().equals(campus.getId())) {
                throw new ForbiddenException("Can only create lessons in your own campus");
            }
            return teacher;
        }
        if (teacherIdFromRequest == null) {
            throw new BadRequestException("teacherId is required for admin");
        }
        Teacher teacher = findTeacherOrThrow(teacherIdFromRequest);
        if (!teacher.getCampus().getId().equals(campus.getId())) {
            throw new BadRequestException("Teacher must belong to the same campus as the lesson");
        }
        return teacher;
    }

    private void checkEditPermission(Lesson lesson, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMIN) {
            return;
        }
        if (principal.getRole() == Role.TEACHER) {
            if (!lesson.getTeacher().getId().equals(principal.getId())) {
                throw new ForbiddenException("Cannot modify another teacher's lesson");
            }
            return;
        }
        throw new ForbiddenException("Access denied");
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

                if (startDateTime.isAfter(OffsetDateTime.now(clock))) {
                    Lesson lesson = lessonMapper.toEntityFromRecurring(request, startDateTime, endDateTime);
                    lesson.setTeacher(teacher);
                    lesson.setCampus(campus);
                    lesson.setWorkoutType(workoutType);
                    lesson.setStatus(LessonStatus.ACTIVE);
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

    LessonResponse toResponse(Lesson lesson) {
        int taken = (int) sheetRepository.countByLessonId(lesson.getId());
        int available = Math.max(0, lesson.getTotalPlaces() - taken);
        ZoneId zoneId = ZoneId.of(lesson.getCampus().getTimezone());
        return new LessonResponse(
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
                lessonMapper.toCampusResponse(lesson.getCampus())
        );
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

    private MyLessonResponse toMyLessonResponseFromLesson(Lesson lesson) {
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
                null
        );
    }

    private Lesson findLessonOrThrow(UUID id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lesson with id: " + id + " was not found"));
    }

    private WorkoutType findActiveWorkoutTypeOrThrow(UUID id) {
        return workoutTypeRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Active workout type with id: " + id + " was not found"));
    }

    private Teacher findTeacherOrThrow(UUID id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Teacher with id: " + id + " was not found"));
    }

    private Student findStudentOrThrow(UUID id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student with id: " + id + " was not found"));
    }

    private Campus findCampusOrThrow(Integer id) {
        return campusRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Campus with id: " + id + " was not found"));
    }

    private Sort resolveSort(Collection<LessonTimeStatus> statuses) {
        boolean onlyPast = statuses != null && statuses.size() == 1 && statuses.iterator().next() == LessonTimeStatus.PAST;
        if (onlyPast) {
            return Sort.by(Sort.Direction.DESC, "startTime");
        }
        return Sort.by(Sort.Direction.ASC, "startTime");
    }
}
