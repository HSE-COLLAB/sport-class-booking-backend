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
import ru.hse.sportclassbookingbackend.model.Campus;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.model.Lesson;
import ru.hse.sportclassbookingbackend.model.Role;
import ru.hse.sportclassbookingbackend.model.Sheet;
import ru.hse.sportclassbookingbackend.model.Student;
import ru.hse.sportclassbookingbackend.model.Teacher;
import ru.hse.sportclassbookingbackend.model.WorkoutType;
import ru.hse.sportclassbookingbackend.repository.LessonRepository;
import ru.hse.sportclassbookingbackend.repository.SheetRepository;
import ru.hse.sportclassbookingbackend.repository.StudentRepository;
import ru.hse.sportclassbookingbackend.security.UserPrincipal;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SheetServiceImplTest {

    private static final Integer CAMPUS_ID = 1;
    private static final Integer OTHER_CAMPUS_ID = 99;
    private static final String CAMPUS_TIMEZONE = "Europe/Moscow";
    private static final int TOTAL_PLACES = 20;
    private static final int PAGE_NUMBER = 0;
    private static final int PAGE_SIZE = 10;

    private static final UUID LESSON_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID WORKOUT_TYPE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TEACHER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OTHER_TEACHER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ADMIN_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID STUDENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID OTHER_STUDENT_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID SHEET_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID OTHER_SHEET_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private static final Instant FROZEN_NOW = Instant.parse("2099-06-01T07:00:00Z");
    private static final OffsetDateTime START_OFFSET_FUTURE =
            OffsetDateTime.ofInstant(FROZEN_NOW.plusSeconds(3600), ZoneOffset.UTC);
    private static final OffsetDateTime END_OFFSET_FUTURE =
            OffsetDateTime.ofInstant(FROZEN_NOW.plusSeconds(7200), ZoneOffset.UTC);
    private static final OffsetDateTime START_OFFSET_PAST =
            OffsetDateTime.ofInstant(FROZEN_NOW.minusSeconds(7200), ZoneOffset.UTC);
    private static final OffsetDateTime END_OFFSET_PAST =
            OffsetDateTime.ofInstant(FROZEN_NOW.minusSeconds(3600), ZoneOffset.UTC);

    @Mock private SheetRepository sheetRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private SheetMapper sheetMapper;
    @Mock private LessonMapper lessonMapper;

    @Spy private Clock clock = Clock.fixed(FROZEN_NOW, ZoneOffset.UTC);

    @InjectMocks private SheetServiceImpl sheetService;

    private Campus campus;
    private HealthGroup healthGroup;
    private WorkoutType workoutType;
    private Teacher teacher;
    private Teacher otherTeacher;
    private Student student;
    private Lesson lesson;
    private Sheet sheet;
    private UserPrincipal adminPrincipal;
    private UserPrincipal teacherPrincipal;
    private UserPrincipal otherTeacherPrincipal;
    private UserPrincipal studentPrincipal;
    private UserPrincipal otherStudentPrincipal;

    @BeforeEach
    void setUp() {
        campus = new Campus();
        campus.setId(CAMPUS_ID);
        campus.setTimezone(CAMPUS_TIMEZONE);

        healthGroup = new HealthGroup();

        workoutType = new WorkoutType();
        workoutType.setId(WORKOUT_TYPE_ID);
        workoutType.setAllowedHealthGroups(Set.of(healthGroup));

        teacher = new Teacher();
        teacher.setId(TEACHER_ID);
        teacher.setCampus(campus);

        otherTeacher = new Teacher();
        otherTeacher.setId(OTHER_TEACHER_ID);
        otherTeacher.setCampus(campus);

        student = new Student();
        student.setId(STUDENT_ID);
        student.setCampus(campus);
        student.setHealthGroup(healthGroup);

        lesson = new Lesson();
        lesson.setId(LESSON_ID);
        lesson.setCampus(campus);
        lesson.setTeacher(teacher);
        lesson.setWorkoutType(workoutType);
        lesson.setStartTime(START_OFFSET_FUTURE);
        lesson.setEndTime(END_OFFSET_FUTURE);
        lesson.setTotalPlaces(TOTAL_PLACES);
        lesson.setStatus(LessonStatus.ACTIVE);

        sheet = new Sheet();
        sheet.setId(SHEET_ID);
        sheet.setLesson(lesson);
        sheet.setStudent(student);
        sheet.setVisited(false);

        adminPrincipal = new UserPrincipal(ADMIN_ID, Role.ADMIN);
        teacherPrincipal = new UserPrincipal(TEACHER_ID, Role.TEACHER);
        otherTeacherPrincipal = new UserPrincipal(OTHER_TEACHER_ID, Role.TEACHER);
        studentPrincipal = new UserPrincipal(STUDENT_ID, Role.STUDENT);
        otherStudentPrincipal = new UserPrincipal(OTHER_STUDENT_ID, Role.STUDENT);
    }

    // ───── register ─────

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("Регистрирует студента на занятие-успехTest")
        void registersStudentSuccessTest() {
            when(lessonRepository.findByIdForUpdate(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(sheetRepository.existsByLessonIdAndStudentId(LESSON_ID, STUDENT_ID)).thenReturn(false);
            when(sheetRepository.countByLessonId(LESSON_ID)).thenReturn(0L);
            when(sheetRepository.hasTimeOverlap(STUDENT_ID, START_OFFSET_FUTURE, END_OFFSET_FUTURE)).thenReturn(false);
            when(sheetRepository.save(any(Sheet.class))).thenAnswer(inv -> {
                Sheet s = inv.getArgument(0);
                s.setId(SHEET_ID);
                return s;
            });

            SheetIdResponse response = sheetService.register(LESSON_ID, studentPrincipal);

            assertThat(response.id()).isEqualTo(SHEET_ID);
            verify(sheetRepository).save(any(Sheet.class));
        }

        @Test
        @DisplayName("Бросает NotFoundException если занятие не найдено-ошибкаTest")
        void throwsNotFoundWhenLessonMissingTest() {
            when(lessonRepository.findByIdForUpdate(LESSON_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sheetService.register(LESSON_ID, studentPrincipal))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(LESSON_ID.toString());
        }

        @Test
        @DisplayName("Бросает ConflictException при регистрации на отменённое занятие-ошибкаTest")
        void throwsConflictWhenLessonCancelledTest() {
            lesson.setStatus(LessonStatus.CANCELLED);
            when(lessonRepository.findByIdForUpdate(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

            assertThatThrownBy(() -> sheetService.register(LESSON_ID, studentPrincipal))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("cancelled lesson");
        }

        @Test
        @DisplayName("Бросает BadRequestException при регистрации на уже начавшееся занятие-ошибкаTest")
        void throwsBadRequestWhenLessonAlreadyStartedTest() {
            lesson.setStartTime(START_OFFSET_PAST);
            lesson.setEndTime(END_OFFSET_PAST);
            when(lessonRepository.findByIdForUpdate(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

            assertThatThrownBy(() -> sheetService.register(LESSON_ID, studentPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already started");
        }

        @Test
        @DisplayName("Бросает BadRequestException если занятие в чужом кампусе-ошибкаTest")
        void throwsBadRequestWhenLessonInAnotherCampusTest() {
            Campus otherCampus = new Campus();
            otherCampus.setId(OTHER_CAMPUS_ID);
            otherCampus.setTimezone(CAMPUS_TIMEZONE);
            student.setCampus(otherCampus);

            when(lessonRepository.findByIdForUpdate(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

            assertThatThrownBy(() -> sheetService.register(LESSON_ID, studentPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("your campus");
        }

        @Test
        @DisplayName("Бросает BadRequestException если workout type не разрешён для health group-ошибкаTest")
        void throwsBadRequestWhenWorkoutTypeNotAllowedForHealthGroupTest() {
            workoutType.setAllowedHealthGroups(Set.of());
            when(lessonRepository.findByIdForUpdate(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

            assertThatThrownBy(() -> sheetService.register(LESSON_ID, studentPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("not allowed for your health group");
        }

        @Test
        @DisplayName("Бросает ConflictException если студент уже записан на занятие-ошибкаTest")
        void throwsConflictWhenAlreadyRegisteredTest() {
            when(lessonRepository.findByIdForUpdate(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(sheetRepository.existsByLessonIdAndStudentId(LESSON_ID, STUDENT_ID)).thenReturn(true);

            assertThatThrownBy(() -> sheetService.register(LESSON_ID, studentPrincipal))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("already registered");
        }

        @Test
        @DisplayName("Бросает ConflictException если не осталось мест-ошибкаTest")
        void throwsConflictWhenNoFreePlacesTest() {
            when(lessonRepository.findByIdForUpdate(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(sheetRepository.existsByLessonIdAndStudentId(LESSON_ID, STUDENT_ID)).thenReturn(false);
            when(sheetRepository.countByLessonId(LESSON_ID)).thenReturn((long) TOTAL_PLACES);

            assertThatThrownBy(() -> sheetService.register(LESSON_ID, studentPrincipal))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("No free places");
        }

        @Test
        @DisplayName("Бросает ConflictException при пересечении с другим занятием студента-ошибкаTest")
        void throwsConflictWhenStudentHasOverlapTest() {
            when(lessonRepository.findByIdForUpdate(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(sheetRepository.existsByLessonIdAndStudentId(LESSON_ID, STUDENT_ID)).thenReturn(false);
            when(sheetRepository.countByLessonId(LESSON_ID)).thenReturn(0L);
            when(sheetRepository.hasTimeOverlap(STUDENT_ID, START_OFFSET_FUTURE, END_OFFSET_FUTURE)).thenReturn(true);

            assertThatThrownBy(() -> sheetService.register(LESSON_ID, studentPrincipal))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("another lesson at this time");
        }
    }

    // ───── cancel ─────

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("Студент отменяет свою запись-успехTest")
        void studentCancelsOwnRegistrationSuccessTest() {
            when(sheetRepository.findById(SHEET_ID)).thenReturn(Optional.of(sheet));

            sheetService.cancel(LESSON_ID, SHEET_ID, studentPrincipal);

            verify(sheetRepository).delete(sheet);
        }

        @Test
        @DisplayName("Админ отменяет любую запись-успехTest")
        void adminCancelsAnyRegistrationSuccessTest() {
            when(sheetRepository.findById(SHEET_ID)).thenReturn(Optional.of(sheet));

            sheetService.cancel(LESSON_ID, SHEET_ID, adminPrincipal);

            verify(sheetRepository).delete(sheet);
        }

        @Test
        @DisplayName("Препод отменяет запись на своё занятие-успехTest")
        void teacherCancelsRegistrationOnOwnLessonSuccessTest() {
            when(sheetRepository.findById(SHEET_ID)).thenReturn(Optional.of(sheet));

            sheetService.cancel(LESSON_ID, SHEET_ID, teacherPrincipal);

            verify(sheetRepository).delete(sheet);
        }

        @Test
        @DisplayName("Бросает NotFoundException если sheet не найден-ошибкаTest")
        void throwsNotFoundWhenSheetMissingTest() {
            when(sheetRepository.findById(SHEET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sheetService.cancel(LESSON_ID, SHEET_ID, studentPrincipal))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(SHEET_ID.toString());
        }

        @Test
        @DisplayName("Бросает BadRequestException если sheet не от этого занятия-ошибкаTest")
        void throwsBadRequestWhenSheetBelongsToAnotherLessonTest() {
            UUID anotherLessonId = UUID.randomUUID();
            when(sheetRepository.findById(SHEET_ID)).thenReturn(Optional.of(sheet));

            assertThatThrownBy(() -> sheetService.cancel(anotherLessonId, SHEET_ID, studentPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        @DisplayName("Бросает ConflictException если занятие отменено-ошибкаTest")
        void throwsConflictWhenLessonCancelledTest() {
            lesson.setStatus(LessonStatus.CANCELLED);
            when(sheetRepository.findById(SHEET_ID)).thenReturn(Optional.of(sheet));

            assertThatThrownBy(() -> sheetService.cancel(LESSON_ID, SHEET_ID, studentPrincipal))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("cancelled lesson");
        }

        @Test
        @DisplayName("Бросает BadRequestException если занятие уже началось-ошибкаTest")
        void throwsBadRequestWhenLessonAlreadyStartedTest() {
            lesson.setStartTime(START_OFFSET_PAST);
            when(sheetRepository.findById(SHEET_ID)).thenReturn(Optional.of(sheet));

            assertThatThrownBy(() -> sheetService.cancel(LESSON_ID, SHEET_ID, studentPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("after the lesson has started");
        }

        @Test
        @DisplayName("Бросает BadRequestException если студент отменяет чужую запись-ошибкаTest")
        void throwsBadRequestWhenStudentCancelsAnothersRegistrationTest() {
            when(sheetRepository.findById(SHEET_ID)).thenReturn(Optional.of(sheet));

            assertThatThrownBy(() -> sheetService.cancel(LESSON_ID, SHEET_ID, otherStudentPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("your own registration");

            verify(sheetRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Бросает BadRequestException если препод отменяет запись на чужое занятие-ошибкаTest")
        void throwsBadRequestWhenTeacherCancelsOnAnothersLessonTest() {
            when(sheetRepository.findById(SHEET_ID)).thenReturn(Optional.of(sheet));

            assertThatThrownBy(() -> sheetService.cancel(LESSON_ID, SHEET_ID, otherTeacherPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("your own lessons");

            verify(sheetRepository, never()).delete(any());
        }
    }

    // ───── getAttendees ─────

    @Nested
    @DisplayName("getAttendees")
    class GetAttendees {

        @Test
        @DisplayName("Возвращает список посетителей-успехTest")
        void returnsAttendeesListSuccessTest() {
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(sheetRepository.findAllByLessonIdWithStudent(LESSON_ID)).thenReturn(List.of(sheet));
            AttendeeResponse expected = new AttendeeResponse(SHEET_ID, false, null);
            when(sheetMapper.toAttendeeResponse(sheet)).thenReturn(expected);

            List<AttendeeResponse> result = sheetService.getAttendees(LESSON_ID);

            assertThat(result).containsExactly(expected);
        }

        @Test
        @DisplayName("Бросает NotFoundException если занятие не найдено-ошибкаTest")
        void throwsNotFoundWhenLessonMissingTest() {
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sheetService.getAttendees(LESSON_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ───── markAttendance ─────

    @Nested
    @DisplayName("markAttendance")
    class MarkAttendance {

        @Test
        @DisplayName("Препод проставляет посещаемость на своё занятие-успехTest")
        void teacherMarksAttendanceOnOwnLessonSuccessTest() {
            lesson.setStartTime(START_OFFSET_PAST);
            lesson.setEndTime(END_OFFSET_PAST);

            Sheet other = new Sheet();
            other.setId(OTHER_SHEET_ID);
            other.setLesson(lesson);

            BulkAttendanceRequest request = new BulkAttendanceRequest(List.of(
                    new AttendanceMark(SHEET_ID, true),
                    new AttendanceMark(OTHER_SHEET_ID, false)
            ));

            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(sheetRepository.findAllByLessonIdWithStudent(LESSON_ID)).thenReturn(List.of(sheet, other));
            when(sheetMapper.toAttendeeResponse(any(Sheet.class)))
                    .thenAnswer(inv -> new AttendeeResponse(((Sheet) inv.getArgument(0)).getId(), null, null));

            List<AttendeeResponse> result = sheetService.markAttendance(LESSON_ID, request, teacherPrincipal);

            assertThat(result).hasSize(2);
            assertThat(sheet.getVisited()).isTrue();
            assertThat(other.getVisited()).isFalse();
            verify(sheetRepository).saveAll(List.of(sheet, other));
        }

        @Test
        @DisplayName("Админ проставляет посещаемость на любое занятие-успехTest")
        void adminMarksAttendanceOnAnyLessonSuccessTest() {
            lesson.setStartTime(START_OFFSET_PAST);
            lesson.setEndTime(END_OFFSET_PAST);

            BulkAttendanceRequest request = new BulkAttendanceRequest(List.of(
                    new AttendanceMark(SHEET_ID, true)
            ));

            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(sheetRepository.findAllByLessonIdWithStudent(LESSON_ID)).thenReturn(List.of(sheet));
            when(sheetMapper.toAttendeeResponse(any(Sheet.class)))
                    .thenReturn(new AttendeeResponse(SHEET_ID, true, null));

            sheetService.markAttendance(LESSON_ID, request, adminPrincipal);

            assertThat(sheet.getVisited()).isTrue();
        }

        @Test
        @DisplayName("Бросает BadRequestException если препод проставляет на чужое занятие-ошибкаTest")
        void throwsBadRequestWhenTeacherMarksOnAnothersLessonTest() {
            lesson.setStartTime(START_OFFSET_PAST);
            BulkAttendanceRequest request = new BulkAttendanceRequest(List.of(
                    new AttendanceMark(SHEET_ID, true)
            ));

            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            assertThatThrownBy(() -> sheetService.markAttendance(LESSON_ID, request, otherTeacherPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("your own lessons");
        }

        @Test
        @DisplayName("Бросает ConflictException если занятие отменено-ошибкаTest")
        void throwsConflictWhenLessonCancelledTest() {
            lesson.setStatus(LessonStatus.CANCELLED);
            lesson.setStartTime(START_OFFSET_PAST);
            BulkAttendanceRequest request = new BulkAttendanceRequest(List.of(
                    new AttendanceMark(SHEET_ID, true)
            ));

            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            assertThatThrownBy(() -> sheetService.markAttendance(LESSON_ID, request, teacherPrincipal))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("cancelled lesson");
        }

        @Test
        @DisplayName("Бросает BadRequestException если занятие ещё не началось-ошибкаTest")
        void throwsBadRequestWhenLessonNotStartedYetTest() {
            // startTime в будущем по умолчанию (см. setUp)
            BulkAttendanceRequest request = new BulkAttendanceRequest(List.of(
                    new AttendanceMark(SHEET_ID, true)
            ));

            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            assertThatThrownBy(() -> sheetService.markAttendance(LESSON_ID, request, teacherPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("before the lesson starts");
        }

        @Test
        @DisplayName("Бросает BadRequestException если в марке указан sheetId не от этого занятия-ошибкаTest")
        void throwsBadRequestWhenMarkContainsAlienSheetIdTest() {
            lesson.setStartTime(START_OFFSET_PAST);
            lesson.setEndTime(END_OFFSET_PAST);

            UUID alienSheetId = UUID.randomUUID();
            BulkAttendanceRequest request = new BulkAttendanceRequest(List.of(
                    new AttendanceMark(alienSheetId, true)
            ));

            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(sheetRepository.findAllByLessonIdWithStudent(LESSON_ID)).thenReturn(List.of(sheet));

            assertThatThrownBy(() -> sheetService.markAttendance(LESSON_ID, request, teacherPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("does not belong to this lesson");
        }
    }

    // ───── getMyLessons ─────

    @Nested
    @DisplayName("getMyLessons")
    class GetMyLessons {

        @Test
        @DisplayName("Возвращает страницу моих занятий-успехTest")
        void returnsMyLessonsPageSuccessTest() {
            Pageable pageable = PageRequest.of(PAGE_NUMBER, PAGE_SIZE);
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(sheetRepository.findAllMyLessons(
                    eq(STUDENT_ID), eq(null), eq(null), eq(null),
                    anyCollection(), any(OffsetDateTime.class), any(Pageable.class)
            )).thenReturn(new PageImpl<>(List.of(sheet)));
            when(sheetRepository.countByLessonId(LESSON_ID)).thenReturn(2L);

            Page<MyLessonResponse> result = sheetService.getMyLessons(
                    null, null, null, null, pageable, studentPrincipal
            );

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Бросает BadRequestException если 'to' не позже 'from'-ошибкаTest")
        void throwsBadRequestWhenToNotAfterFromTest() {
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            Pageable pageable = PageRequest.of(PAGE_NUMBER, PAGE_SIZE);
            LocalDateTime from = LocalDateTime.of(2099, 6, 1, 12, 0);
            LocalDateTime to = LocalDateTime.of(2099, 6, 1, 10, 0);

            assertThatThrownBy(() -> sheetService.getMyLessons(null, null, from, to, pageable, studentPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("'to' must be after 'from'");
        }

        @Test
        @DisplayName("Сортирует по убыванию lesson.startTime если запрошены только PAST-успехTest")
        void sortsDescWhenOnlyPastStatusTest() {
            Pageable pageable = PageRequest.of(PAGE_NUMBER, PAGE_SIZE);
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(sheetRepository.findAllMyLessons(
                    any(), any(), any(), any(),
                    anyCollection(), any(OffsetDateTime.class), any(Pageable.class)
            )).thenReturn(new PageImpl<>(List.of()));

            sheetService.getMyLessons(
                    List.of(LessonTimeStatus.PAST), null, null, null, pageable, studentPrincipal
            );

            verify(sheetRepository).findAllMyLessons(
                    any(), any(), any(), any(),
                    anyCollection(), any(OffsetDateTime.class),
                    argThat((Pageable p) -> p.getSort().equals(Sort.by(Sort.Direction.DESC, "lesson.startTime")))
            );
        }

        @Test
        @DisplayName("Бросает BadRequestException если студент не найден-ошибкаTest")
        void throwsBadRequestWhenStudentMissingTest() {
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());
            Pageable pageable = PageRequest.of(PAGE_NUMBER, PAGE_SIZE);

            assertThatThrownBy(() -> sheetService.getMyLessons(null, null, null, null, pageable, studentPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Student with id");
        }
    }
}