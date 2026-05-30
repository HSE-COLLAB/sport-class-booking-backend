package ru.hse.sportclassbookingbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonPatchRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonResponse;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonStatus;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonTimeStatus;
import ru.hse.sportclassbookingbackend.dto.lesson.RecurringLessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.RecurringLessonResponse;
import ru.hse.sportclassbookingbackend.exception.BadRequestException;
import ru.hse.sportclassbookingbackend.exception.ConflictException;
import ru.hse.sportclassbookingbackend.exception.ForbiddenException;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.LessonMapper;
import ru.hse.sportclassbookingbackend.model.Campus;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.model.Lesson;
import ru.hse.sportclassbookingbackend.model.Role;
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceImplTest {

    private static final Integer CAMPUS_ID = 1;
    private static final Integer OTHER_CAMPUS_ID = 99;
    private static final String CAMPUS_TIMEZONE = "Europe/Moscow";
    private static final Integer HEALTH_GROUP_ID = 10;
    private static final int TOTAL_PLACES = 20;
    private static final int PAGE_NUMBER = 0;
    private static final int PAGE_SIZE = 10;

    private static final UUID LESSON_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID WORKOUT_TYPE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TEACHER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OTHER_TEACHER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ADMIN_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID STUDENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private static final Instant FROZEN_NOW = Instant.parse("2099-06-01T07:00:00Z");
    private static final LocalDateTime START_LOCAL = LocalDateTime.of(2099, 6, 1, 13, 0);
    private static final LocalDateTime END_LOCAL = LocalDateTime.of(2099, 6, 1, 14, 0);
    private static final OffsetDateTime START_OFFSET = START_LOCAL.atOffset(ZoneOffset.ofHours(3));
    private static final OffsetDateTime END_OFFSET = END_LOCAL.atOffset(ZoneOffset.ofHours(3));

    @Mock private LessonRepository lessonRepository;
    @Mock private WorkoutTypeRepository workoutTypeRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private CampusRepository campusRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private SheetRepository sheetRepository;
    @Mock private LessonMapper lessonMapper;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Spy private Clock clock = Clock.fixed(FROZEN_NOW, ZoneOffset.UTC);

    @InjectMocks private LessonServiceImpl lessonService;

    private Campus campus;
    private WorkoutType workoutType;
    private Teacher teacher;
    private Teacher otherTeacher;
    private Lesson lesson;
    private UserPrincipal adminPrincipal;
    private UserPrincipal teacherPrincipal;
    private UserPrincipal studentPrincipal;

    @BeforeEach
    void setUp() {
        campus = new Campus();
        campus.setId(CAMPUS_ID);
        campus.setTimezone(CAMPUS_TIMEZONE);

        workoutType = new WorkoutType();
        workoutType.setId(WORKOUT_TYPE_ID);

        teacher = new Teacher();
        teacher.setId(TEACHER_ID);
        teacher.setCampus(campus);

        otherTeacher = new Teacher();
        otherTeacher.setId(OTHER_TEACHER_ID);
        otherTeacher.setCampus(campus);

        lesson = new Lesson();
        lesson.setId(LESSON_ID);
        lesson.setCampus(campus);
        lesson.setTeacher(teacher);
        lesson.setWorkoutType(workoutType);
        lesson.setStartTime(START_OFFSET);
        lesson.setEndTime(END_OFFSET);
        lesson.setTotalPlaces(TOTAL_PLACES);
        lesson.setStatus(LessonStatus.ACTIVE);

        adminPrincipal = new UserPrincipal(ADMIN_ID, Role.ADMIN);
        teacherPrincipal = new UserPrincipal(TEACHER_ID, Role.TEACHER);
        studentPrincipal = new UserPrincipal(STUDENT_ID, Role.STUDENT);
    }

    // ───── getAll ─────

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("Возвращает страницу занятий с дефолтными статусами-успехTest")
        void returnsPageWithDefaultStatusesTest() {
            Pageable pageable = PageRequest.of(PAGE_NUMBER, PAGE_SIZE);
            Page<Lesson> lessonPage = new PageImpl<>(List.of(lesson));

            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(lessonRepository.findAllWithFilters(
                    eq(CAMPUS_ID), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null),
                    anyCollection(), eq(true), any(OffsetDateTime.class), any(Pageable.class)
            )).thenReturn(lessonPage);
            when(sheetRepository.countByLessonId(LESSON_ID)).thenReturn(5L);

            Page<LessonResponse> result = lessonService.getAll(
                    CAMPUS_ID, null, null, null, null, null, null, null, null, pageable, adminPrincipal
            );

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Бросает BadRequestException если 'to' раньше или равно 'from'-ошибкаTest")
        void throwsBadRequestWhenToNotAfterFromTest() {
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            Pageable pageable = PageRequest.of(PAGE_NUMBER, PAGE_SIZE);
            LocalDateTime from = LocalDateTime.of(2099, 6, 1, 12, 0);
            LocalDateTime to = LocalDateTime.of(2099, 6, 1, 10, 0);

            assertThatThrownBy(() -> lessonService.getAll(
                    CAMPUS_ID, null, null, from, to, null, null, null, null, pageable, adminPrincipal
            )).isInstanceOf(BadRequestException.class).hasMessageContaining("'to' must be after 'from'");
        }

        @Test
        @DisplayName("Бросает ForbiddenException если myHealthGroup=true и роль не STUDENT-ошибкаTest")
        void throwsBadRequestWhenMyHealthGroupForNonStudentTest() {
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            Pageable pageable = PageRequest.of(PAGE_NUMBER, PAGE_SIZE);

            assertThatThrownBy(() -> lessonService.getAll(
                    CAMPUS_ID, null, null, null, null, null, true, null, null, pageable, adminPrincipal
            )).isInstanceOf(ForbiddenException.class).hasMessageContaining("only available to students");
        }

        @Test
        @DisplayName("Фильтрует по health group студента если myHealthGroup=true-успехTest")
        void filtersByStudentHealthGroupTest() {
            HealthGroup healthGroup = new HealthGroup();
            healthGroup.setId(HEALTH_GROUP_ID);
            Student student = new Student();
            student.setId(STUDENT_ID);
            student.setHealthGroup(healthGroup);

            Pageable pageable = PageRequest.of(PAGE_NUMBER, PAGE_SIZE);
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(lessonRepository.findAllWithFilters(
                    any(), any(), any(), any(), any(), any(), eq(HEALTH_GROUP_ID),
                    anyCollection(), any(Boolean.class), any(OffsetDateTime.class), any(Pageable.class)
            )).thenReturn(new PageImpl<>(List.of()));

            lessonService.getAll(CAMPUS_ID, null, null, null, null, null, true, null, null, pageable, studentPrincipal);

            verify(lessonRepository).findAllWithFilters(
                    any(), any(), any(), any(), any(), any(), eq(HEALTH_GROUP_ID),
                    anyCollection(), any(Boolean.class), any(OffsetDateTime.class), any(Pageable.class)
            );
        }

        @Test
        @DisplayName("Сортирует по убыванию startTime если запрошены только PAST занятия-успехTest")
        void sortsDescWhenOnlyPastStatusTest() {
            Pageable pageable = PageRequest.of(PAGE_NUMBER, PAGE_SIZE);
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(lessonRepository.findAllWithFilters(
                    any(), any(), any(), any(), any(), any(), any(),
                    anyCollection(), any(Boolean.class), any(OffsetDateTime.class), any(Pageable.class)
            )).thenReturn(new PageImpl<>(List.of()));

            lessonService.getAll(
                    CAMPUS_ID, null, null, null, null, null, null,
                    List.of(LessonTimeStatus.PAST), null, pageable, adminPrincipal
            );

            verify(lessonRepository).findAllWithFilters(
                    any(), any(), any(), any(), any(), any(), any(),
                    anyCollection(), any(Boolean.class), any(OffsetDateTime.class),
                    argThat((Pageable p) -> p.getSort().equals(Sort.by(Sort.Direction.DESC, "startTime")))
            );
        }
    }

    // ───── getById ─────

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("Возвращает занятие по id-успехTest")
        void returnsLessonByIdSuccessTest() {
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(sheetRepository.countByLessonId(LESSON_ID)).thenReturn(3L);

            LessonResponse result = lessonService.getById(LESSON_ID);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Бросает NotFoundException если занятие не найдено-ошибкаTest")
        void throwsNotFoundWhenLessonMissingTest() {
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lessonService.getById(LESSON_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(LESSON_ID.toString());
        }
    }

    // ───── create ─────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Создаёт занятие админом-успехTest")
        void createsLessonByAdminSuccessTest() {
            LessonRequest request = lessonRequest(TEACHER_ID);

            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID)).thenReturn(Optional.of(workoutType));
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
            when(lessonRepository.hasTeacherTimeOverlap(eq(TEACHER_ID), any(), any(), eq(null))).thenReturn(false);
            when(lessonMapper.toEntity(request)).thenReturn(lesson);
            when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sheetRepository.countByLessonId(any())).thenReturn(0L);

            lessonService.create(request, adminPrincipal);

            verify(lessonRepository).save(any(Lesson.class));
        }

        @Test
        @DisplayName("Бросает BadRequestException если endTime не позже startTime-ошибкаTest")
        void throwsBadRequestWhenEndTimeNotAfterStartTimeTest() {
            LessonRequest request = new LessonRequest(
                    "Title", "Place", END_LOCAL, START_LOCAL, TOTAL_PLACES,
                    WORKOUT_TYPE_ID, CAMPUS_ID, TEACHER_ID, null
            );

            assertThatThrownBy(() -> lessonService.create(request, adminPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("endTime must be after startTime");
        }

        @Test
        @DisplayName("Бросает ConflictException при пересечении с занятием препода-ошибкаTest")
        void throwsConflictWhenTeacherHasOverlapTest() {
            LessonRequest request = lessonRequest(TEACHER_ID);

            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID)).thenReturn(Optional.of(workoutType));
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
            when(lessonRepository.hasTeacherTimeOverlap(eq(TEACHER_ID), any(), any(), eq(null))).thenReturn(true);

            assertThatThrownBy(() -> lessonService.create(request, adminPrincipal))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Teacher already has a lesson");
        }

        @Test
        @DisplayName("Бросает NotFoundException если кампуса нет-ошибкаTest")
        void throwsBadRequestWhenCampusMissingTest() {
            LessonRequest request = lessonRequest(TEACHER_ID);
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lessonService.create(request, adminPrincipal))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Campus with id");
        }

        @Test
        @DisplayName("Бросает NotFoundException если workout type не активен-ошибкаTest")
        void throwsBadRequestWhenWorkoutTypeInactiveTest() {
            LessonRequest request = lessonRequest(TEACHER_ID);
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lessonService.create(request, adminPrincipal))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Active workout type");
        }

        @Test
        @DisplayName("Бросает BadRequestException если админ не передал teacherId-ошибкаTest")
        void throwsBadRequestWhenAdminDidNotProvideTeacherIdTest() {
            LessonRequest request = lessonRequest(null);
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID)).thenReturn(Optional.of(workoutType));

            assertThatThrownBy(() -> lessonService.create(request, adminPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("teacherId is required for admin");
        }

        @Test
        @DisplayName("Бросает ForbiddenException если препод указал чужой teacherId-ошибкаTest")
        void throwsBadRequestWhenTeacherUsesAnotherTeacherIdTest() {
            LessonRequest request = lessonRequest(OTHER_TEACHER_ID);
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID)).thenReturn(Optional.of(workoutType));

            assertThatThrownBy(() -> lessonService.create(request, teacherPrincipal))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("another teacher");
        }

        @Test
        @DisplayName("Бросает ForbiddenException если препод создаёт занятие в чужом кампусе-ошибкаTest")
        void throwsBadRequestWhenTeacherInWrongCampusTest() {
            Campus otherCampus = new Campus();
            otherCampus.setId(OTHER_CAMPUS_ID);
            otherCampus.setTimezone(CAMPUS_TIMEZONE);
            teacher.setCampus(otherCampus);

            LessonRequest request = lessonRequest(TEACHER_ID);
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID)).thenReturn(Optional.of(workoutType));
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));

            assertThatThrownBy(() -> lessonService.create(request, teacherPrincipal))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("your own campus");
        }

        @Test
        @DisplayName("Бросает BadRequestException если препод из admin-запроса не из того кампуса-ошибкаTest")
        void throwsBadRequestWhenAdminPickedTeacherFromAnotherCampusTest() {
            Campus otherCampus = new Campus();
            otherCampus.setId(OTHER_CAMPUS_ID);
            teacher.setCampus(otherCampus);

            LessonRequest request = lessonRequest(TEACHER_ID);
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID)).thenReturn(Optional.of(workoutType));
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));

            assertThatThrownBy(() -> lessonService.create(request, adminPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("must belong to the same campus");
        }
    }

    // ───── createRecurring ─────

    @Nested
    @DisplayName("createRecurring")
    class CreateRecurring {

        @Test
        @DisplayName("Создаёт серию занятий-успехTest")
        void createsRecurringSeriesSuccessTest() {
            // FROZEN_NOW = 2099-06-01 07:00 UTC, берём даты явно в будущем относительно этого
            RecurringLessonRequest request = recurringRequest(
                    LocalDate.of(2099, 6, 2), LocalDate.of(2099, 6, 16),
                    Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
            );

            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID)).thenReturn(Optional.of(workoutType));
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
            when(lessonMapper.toEntityFromRecurring(eq(request), any(), any())).thenAnswer(inv -> {
                Lesson l = new Lesson();
                l.setStartTime(inv.getArgument(1));
                l.setEndTime(inv.getArgument(2));
                return l;
            });
            when(lessonRepository.hasTeacherTimeOverlap(any(), any(), any(), eq(null))).thenReturn(false);

            RecurringLessonResponse response = lessonService.createRecurring(request, adminPrincipal);

            assertThat(response.count()).isPositive();
            verify(lessonRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Бросает BadRequestException если endTime не позже startTime-ошибкаTest")
        void throwsBadRequestWhenEndTimeNotAfterStartTimeTest() {
            RecurringLessonRequest request = new RecurringLessonRequest(
                    "Title", "Place",
                    LocalTime.of(11, 0), LocalTime.of(10, 0),
                    TOTAL_PLACES, WORKOUT_TYPE_ID, null,
                    Set.of(DayOfWeek.MONDAY),
                    LocalDate.of(2099, 6, 2), LocalDate.of(2099, 6, 16),
                    CAMPUS_ID, TEACHER_ID
            );

            assertThatThrownBy(() -> lessonService.createRecurring(request, adminPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("endTime must be after startTime");
        }

        @Test
        @DisplayName("Бросает BadRequestException если endDate раньше startDate-ошибкаTest")
        void throwsBadRequestWhenEndDateBeforeStartDateTest() {
            RecurringLessonRequest request = new RecurringLessonRequest(
                    "Title", "Place",
                    LocalTime.of(10, 0), LocalTime.of(11, 0),
                    TOTAL_PLACES, WORKOUT_TYPE_ID, null,
                    Set.of(DayOfWeek.MONDAY),
                    LocalDate.of(2099, 6, 20), LocalDate.of(2099, 6, 10),
                    CAMPUS_ID, TEACHER_ID
            );

            assertThatThrownBy(() -> lessonService.createRecurring(request, adminPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("endDate must not be before startDate");
        }

        @Test
        @DisplayName("Бросает BadRequestException если диапазон больше 365 дней-ошибкаTest")
        void throwsBadRequestWhenRangeExceedsMaxDaysTest() {
            RecurringLessonRequest request = new RecurringLessonRequest(
                    "Title", "Place",
                    LocalTime.of(10, 0), LocalTime.of(11, 0),
                    TOTAL_PLACES, WORKOUT_TYPE_ID, null,
                    Set.of(DayOfWeek.MONDAY),
                    LocalDate.of(2099, 6, 2), LocalDate.of(2102, 6, 2),
                    CAMPUS_ID, TEACHER_ID
            );

            assertThatThrownBy(() -> lessonService.createRecurring(request, adminPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("365 days");
        }

        @Test
        @DisplayName("Бросает BadRequestException если ни один день недели не попал-ошибкаTest")
        void throwsBadRequestWhenNoLessonsGeneratedTest() {
            // 2099-06-02 = вторник, 2099-06-03 = среда. Просим только воскресенье — пусто.
            RecurringLessonRequest request = recurringRequest(
                    LocalDate.of(2099, 6, 2), LocalDate.of(2099, 6, 3),
                    Set.of(DayOfWeek.SUNDAY)
            );

            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID)).thenReturn(Optional.of(workoutType));
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));

            assertThatThrownBy(() -> lessonService.createRecurring(request, adminPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("No lessons could be created");
        }

        @Test
        @DisplayName("Бросает ConflictException при пересечении одного из сгенерированных занятий-ошибкаTest")
        void throwsConflictWhenAnyGeneratedLessonOverlapsTest() {
            RecurringLessonRequest request = recurringRequest(
                    LocalDate.of(2099, 6, 2), LocalDate.of(2099, 6, 16),
                    Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
            );

            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID)).thenReturn(Optional.of(workoutType));
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
            when(lessonMapper.toEntityFromRecurring(eq(request), any(), any())).thenAnswer(inv -> {
                Lesson l = new Lesson();
                l.setStartTime(inv.getArgument(1));
                l.setEndTime(inv.getArgument(2));
                return l;
            });
            when(lessonRepository.hasTeacherTimeOverlap(any(), any(), any(), eq(null))).thenReturn(true);

            assertThatThrownBy(() -> lessonService.createRecurring(request, adminPrincipal))
                    .isInstanceOf(ConflictException.class);
        }
    }

    // ───── update ─────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Обновляет занятие админом-успехTest")
        void updatesLessonByAdminSuccessTest() {
            LessonPatchRequest request = new LessonPatchRequest(
                    "New title", null, null, null, null, null, null, null, null
            );

            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sheetRepository.countByLessonId(LESSON_ID)).thenReturn(0L);

            lessonService.update(LESSON_ID, request, adminPrincipal);

            verify(lessonMapper).updateFromPatch(request, lesson);
            verify(lessonRepository).save(lesson);
        }

        @Test
        @DisplayName("Бросает ConflictException при попытке редактировать отменённое занятие-ошибкаTest")
        void throwsConflictWhenEditingCancelledLessonTest() {
            lesson.setStatus(LessonStatus.CANCELLED);
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            LessonPatchRequest request = new LessonPatchRequest(
                    "Title", null, null, null, null, null, null, null, null
            );

            assertThatThrownBy(() -> lessonService.update(LESSON_ID, request, adminPrincipal))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("cancelled");
        }

        @Test
        @DisplayName("Бросает ForbiddenException если препод редактирует чужое занятие-ошибкаTest")
        void throwsBadRequestWhenTeacherEditsAnotherTeachersLessonTest() {
            lesson.setTeacher(otherTeacher);
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            LessonPatchRequest request = new LessonPatchRequest(
                    "Title", null, null, null, null, null, null, null, null
            );

            assertThatThrownBy(() -> lessonService.update(LESSON_ID, request, teacherPrincipal))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("another teacher");
        }

        @Test
        @DisplayName("Бросает ForbiddenException если update от студента-ошибкаTest")
        void throwsBadRequestWhenStudentUpdatesTest() {
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            LessonPatchRequest request = new LessonPatchRequest(
                    "Title", null, null, null, null, null, null, null, null
            );

            assertThatThrownBy(() -> lessonService.update(LESSON_ID, request, studentPrincipal))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Access denied");
        }

        @Test
        @DisplayName("Обновляет workoutType если в патче передан workoutTypeId-успехTest")
        void updatesWorkoutTypeWhenProvidedTest() {
            UUID newWorkoutTypeId = UUID.randomUUID();
            WorkoutType newWorkoutType = new WorkoutType();
            newWorkoutType.setId(newWorkoutTypeId);

            LessonPatchRequest request = new LessonPatchRequest(
                    null, null, null, null, null, newWorkoutTypeId, null, null, null
            );

            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(workoutTypeRepository.findByIdAndIsActiveTrue(newWorkoutTypeId)).thenReturn(Optional.of(newWorkoutType));
            when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sheetRepository.countByLessonId(LESSON_ID)).thenReturn(0L);

            lessonService.update(LESSON_ID, request, adminPrincipal);

            assertThat(lesson.getWorkoutType()).isEqualTo(newWorkoutType);
        }

        @Test
        @DisplayName("Бросает ForbiddenException если препод меняет кампус на чужой-ошибкаTest")
        void throwsBadRequestWhenTeacherChangesToAnotherCampusTest() {
            Campus anotherCampus = new Campus();
            anotherCampus.setId(OTHER_CAMPUS_ID);
            anotherCampus.setTimezone(CAMPUS_TIMEZONE);

            LessonPatchRequest request = new LessonPatchRequest(
                    null, null, null, null, null, null, OTHER_CAMPUS_ID, null, null
            );

            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(campusRepository.findById(OTHER_CAMPUS_ID)).thenReturn(Optional.of(anotherCampus));
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));

            assertThatThrownBy(() -> lessonService.update(LESSON_ID, request, teacherPrincipal))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("your own campus");
        }

        @Test
        @DisplayName("Бросает ForbiddenException если препод пытается переназначить препода-ошибкаTest")
        void throwsBadRequestWhenTeacherReassignsTeacherTest() {
            LessonPatchRequest request = new LessonPatchRequest(
                    null, null, null, null, null, null, null, OTHER_TEACHER_ID, null
            );

            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            assertThatThrownBy(() -> lessonService.update(LESSON_ID, request, teacherPrincipal))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("reassign");
        }

        @Test
        @DisplayName("Бросает BadRequestException если новый препод из другого кампуса-ошибкаTest")
        void throwsBadRequestWhenNewTeacherFromAnotherCampusTest() {
            Campus otherCampus = new Campus();
            otherCampus.setId(OTHER_CAMPUS_ID);
            otherTeacher.setCampus(otherCampus);

            LessonPatchRequest request = new LessonPatchRequest(
                    null, null, null, null, null, null, null, OTHER_TEACHER_ID, null
            );

            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(teacherRepository.findById(OTHER_TEACHER_ID)).thenReturn(Optional.of(otherTeacher));

            assertThatThrownBy(() -> lessonService.update(LESSON_ID, request, adminPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("same campus");
        }

        @Test
        @DisplayName("Бросает BadRequestException если новый endTime не позже нового startTime-ошибкаTest")
        void throwsBadRequestWhenNewTimeRangeInvalidTest() {
            LessonPatchRequest request = new LessonPatchRequest(
                    null, null, END_LOCAL, START_LOCAL, null, null, null, null, null
            );

            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            assertThatThrownBy(() -> lessonService.update(LESSON_ID, request, adminPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("endTime must be after startTime");
        }

        @Test
        @DisplayName("Бросает ConflictException при пересечении после смены времени-ошибкаTest")
        void throwsConflictWhenNewTimeOverlapsTest() {
            LessonPatchRequest request = new LessonPatchRequest(
                    null, null, START_LOCAL.plusHours(1), END_LOCAL.plusHours(1),
                    null, null, null, null, null
            );

            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(lessonRepository.hasTeacherTimeOverlap(eq(TEACHER_ID), any(), any(), eq(LESSON_ID))).thenReturn(true);

            assertThatThrownBy(() -> lessonService.update(LESSON_ID, request, adminPrincipal))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Teacher already has");
        }

        @Test
        @DisplayName("Бросает ConflictException если totalPlaces меньше уже записавшихся-ошибкаTest")
        void throwsConflictWhenTotalPlacesLessThanTakenTest() {
            LessonPatchRequest request = new LessonPatchRequest(
                    null, null, null, null, 5, null, null, null, null
            );

            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(sheetRepository.countByLessonId(LESSON_ID)).thenReturn(10L);

            assertThatThrownBy(() -> lessonService.update(LESSON_ID, request, adminPrincipal))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("10 students are already registered");
        }

        @Test
        @DisplayName("Бросает NotFoundException если занятие не найдено-ошибкаTest")
        void throwsNotFoundWhenLessonMissingTest() {
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());
            LessonPatchRequest request = new LessonPatchRequest(
                    "T", null, null, null, null, null, null, null, null
            );

            assertThatThrownBy(() -> lessonService.update(LESSON_ID, request, adminPrincipal))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ───── cancel ─────

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("Отменяет активное занятие админом-успехTest")
        void cancelsActiveLessonByAdminSuccessTest() {
            // endTime в будущем относительно FROZEN_NOW
            lesson.setEndTime(OffsetDateTime.ofInstant(FROZEN_NOW.plusSeconds(3600), ZoneOffset.UTC));
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sheetRepository.countByLessonId(LESSON_ID)).thenReturn(0L);

            lessonService.cancel(LESSON_ID, adminPrincipal);

            assertThat(lesson.getStatus()).isEqualTo(LessonStatus.CANCELLED);
        }

        @Test
        @DisplayName("Бросает ConflictException при попытке отменить уже отменённое-ошибкаTest")
        void throwsConflictWhenAlreadyCancelledTest() {
            lesson.setStatus(LessonStatus.CANCELLED);
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            assertThatThrownBy(() -> lessonService.cancel(LESSON_ID, adminPrincipal))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("already cancelled");
        }

        @Test
        @DisplayName("Бросает ConflictException при отмене уже закончившегося занятия-ошибкаTest")
        void throwsBadRequestWhenCancellingPastLessonTest() {
            lesson.setEndTime(OffsetDateTime.ofInstant(FROZEN_NOW.minusSeconds(1), ZoneOffset.UTC));
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            assertThatThrownBy(() -> lessonService.cancel(LESSON_ID, adminPrincipal))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("already ended");
        }

        @Test
        @DisplayName("Препод не может отменить чужое занятие-ошибкаTest")
        void teacherCannotCancelAnotherTeachersLessonTest() {
            lesson.setTeacher(otherTeacher);
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            assertThatThrownBy(() -> lessonService.cancel(LESSON_ID, teacherPrincipal))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("another teacher");

            verify(lessonRepository, never()).save(any());
        }
    }

    // ───── helpers ─────

    private LessonRequest lessonRequest(UUID teacherId) {
        return new LessonRequest(
                "Title", "Place", START_LOCAL, END_LOCAL, TOTAL_PLACES,
                WORKOUT_TYPE_ID, CAMPUS_ID, teacherId, null
        );
    }

    private RecurringLessonRequest recurringRequest(LocalDate startDate, LocalDate endDate, Set<DayOfWeek> days) {
        return new RecurringLessonRequest(
                "Title", "Place",
                LocalTime.of(10, 0), LocalTime.of(11, 0),
                TOTAL_PLACES, WORKOUT_TYPE_ID, null,
                days, startDate, endDate,
                CAMPUS_ID, TEACHER_ID
        );
    }
}