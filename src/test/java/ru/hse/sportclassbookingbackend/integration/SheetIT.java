package ru.hse.sportclassbookingbackend.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import ru.hse.sportclassbookingbackend.AbstractIntegrationTest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonStatus;
import ru.hse.sportclassbookingbackend.dto.sheet.AttendanceMark;
import ru.hse.sportclassbookingbackend.dto.sheet.BulkAttendanceRequest;
import ru.hse.sportclassbookingbackend.dto.sheet.SheetIdResponse;
import ru.hse.sportclassbookingbackend.model.Campus;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.model.Lesson;
import ru.hse.sportclassbookingbackend.model.Sheet;
import ru.hse.sportclassbookingbackend.model.Student;
import ru.hse.sportclassbookingbackend.model.StudentGroup;
import ru.hse.sportclassbookingbackend.model.Teacher;
import ru.hse.sportclassbookingbackend.model.WorkoutType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SheetIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@hse.ru";
    private static final String TEACHER_EMAIL = "teacher@hse.ru";
    private static final String OTHER_TEACHER_EMAIL = "other.teacher@hse.ru";
    private static final String STUDENT_EMAIL = "student@hse.ru";
    private static final String OTHER_STUDENT_EMAIL = "other.student@hse.ru";

    private static final int TOTAL_PLACES = 2;

    private Campus campus;
    private HealthGroup healthGroup;
    private StudentGroup studentGroup;
    private WorkoutType workoutType;
    private Teacher teacher;
    private Student student;

    private String adminToken;
    private String teacherToken;
    private String otherTeacherToken;
    private String studentToken;
    private String otherStudentToken;

    @BeforeEach
    void setUpFixtures() throws Exception {
        campus = campusRepository.findById(NN_CAMPUS_ID).orElseThrow();
        healthGroup = healthGroupRepository.findById(DEFAULT_HEALTH_GROUP_ID).orElseThrow();
        studentGroup = createDefaultStudentGroup();

        workoutType = new WorkoutType();
        workoutType.setTitle("Волейбол");
        workoutType.setIsActive(true);
        workoutType.setAllowedHealthGroups(Set.of(healthGroup));
        workoutType = workoutTypeRepository.save(workoutType);

        createAdmin(ADMIN_EMAIL);
        teacher = createTeacher(TEACHER_EMAIL, campus);
        createTeacher(OTHER_TEACHER_EMAIL, campus);
        student = createStudent(STUDENT_EMAIL, campus, healthGroup, studentGroup);
        createStudent(OTHER_STUDENT_EMAIL, campus, healthGroup, studentGroup);

        adminToken = loginAndGetToken(ADMIN_EMAIL);
        teacherToken = loginAndGetToken(TEACHER_EMAIL);
        otherTeacherToken = loginAndGetToken(OTHER_TEACHER_EMAIL);
        studentToken = loginAndGetToken(STUDENT_EMAIL);
        otherStudentToken = loginAndGetToken(OTHER_STUDENT_EMAIL);
    }

    // ───── POST /lessons/{lessonId}/sheets ─────

    @Nested
    @DisplayName("POST /lessons/{lessonId}/sheets")
    class Register {

        @Test
        @DisplayName("Студент регистрируется на занятие-успехTest")
        void studentRegistersOnLessonTest() throws Exception {
            Lesson lesson = createFutureLesson();

            mockMvc.perform(post("/lessons/{lessonId}/sheets", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists());
        }

        @Test
        @DisplayName("Повторная регистрация на то же занятие возвращает 409-ошибкаTest")
        void doubleRegistrationReturns409Test() throws Exception {
            Lesson lesson = createFutureLesson();

            mockMvc.perform(post("/lessons/{lessonId}/sheets", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/lessons/{lessonId}/sheets", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errors[0]", containsString("already registered")));
        }

        @Test
        @DisplayName("Регистрация на занятие без мест возвращает 409-ошибкаTest")
        void registrationOnFullLessonReturns409Test() throws Exception {
            Lesson lesson = createFutureLesson();

            mockMvc.perform(post("/lessons/{lessonId}/sheets", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/lessons/{lessonId}/sheets", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(otherStudentToken)))
                    .andExpect(status().isCreated());

            createStudent("third@hse.ru", campus, healthGroup, studentGroup);
            String thirdToken = loginAndGetToken("third@hse.ru");

            mockMvc.perform(post("/lessons/{lessonId}/sheets", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(thirdToken)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errors[0]", containsString("No free places")));
        }

        @Test
        @DisplayName("Препод получает 403 при попытке зарегистрироваться-ошибкаTest")
        void teacherGets403OnRegisterTest() throws Exception {
            Lesson lesson = createFutureLesson();

            mockMvc.perform(post("/lessons/{lessonId}/sheets", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(teacherToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Регистрация на несуществующее занятие возвращает 404-ошибкаTest")
        void registrationOnMissingLessonReturns404Test() throws Exception {
            mockMvc.perform(post("/lessons/{lessonId}/sheets", UUID.randomUUID())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Регистрация на отменённое занятие возвращает 409-ошибкаTest")
        void registrationOnCancelledLessonReturns409Test() throws Exception {
            Lesson lesson = createFutureLesson();
            lesson.setStatus(LessonStatus.CANCELLED);
            lessonRepository.save(lesson);

            mockMvc.perform(post("/lessons/{lessonId}/sheets", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errors[0]", containsString("cancelled")));
        }
    }

    // ───── DELETE /lessons/{lessonId}/sheets/{sheetId} ─────

    @Nested
    @DisplayName("DELETE /lessons/{lessonId}/sheets/{sheetId}")
    class Cancel {

        @Test
        @DisplayName("Студент отменяет свою запись-успехTest")
        void studentCancelsOwnRegistrationTest() throws Exception {
            Lesson lesson = createFutureLesson();
            UUID sheetId = registerStudent(lesson.getId(), studentToken);

            mockMvc.perform(delete("/lessons/{lessonId}/sheets/{sheetId}", lesson.getId(), sheetId)
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Студент НЕ может отменить чужую запись-ошибкаTest")
        void studentCannotCancelAnothersRegistrationTest() throws Exception {
            Lesson lesson = createFutureLesson();
            UUID sheetId = registerStudent(lesson.getId(), studentToken);

            mockMvc.perform(delete("/lessons/{lessonId}/sheets/{sheetId}", lesson.getId(), sheetId)
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(otherStudentToken)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]", containsString("your own registration")));
        }

        @Test
        @DisplayName("Препод отменяет запись на своё занятие-успехTest")
        void teacherCancelsRegistrationOnOwnLessonTest() throws Exception {
            Lesson lesson = createFutureLesson();
            UUID sheetId = registerStudent(lesson.getId(), studentToken);

            mockMvc.perform(delete("/lessons/{lessonId}/sheets/{sheetId}", lesson.getId(), sheetId)
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(teacherToken)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Препод НЕ может отменить запись на чужое занятие-ошибкаTest")
        void teacherCannotCancelOnAnothersLessonTest() throws Exception {
            Lesson lesson = createFutureLesson();
            UUID sheetId = registerStudent(lesson.getId(), studentToken);

            mockMvc.perform(delete("/lessons/{lessonId}/sheets/{sheetId}", lesson.getId(), sheetId)
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(otherTeacherToken)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]", containsString("your own lessons")));
        }

        @Test
        @DisplayName("Админ отменяет любую запись-успехTest")
        void adminCancelsAnyRegistrationTest() throws Exception {
            Lesson lesson = createFutureLesson();
            UUID sheetId = registerStudent(lesson.getId(), studentToken);

            mockMvc.perform(delete("/lessons/{lessonId}/sheets/{sheetId}", lesson.getId(), sheetId)
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(adminToken)))
                    .andExpect(status().isNoContent());
        }
    }

    // ───── GET /lessons/{lessonId}/sheets ─────

    @Nested
    @DisplayName("GET /lessons/{lessonId}/sheets")
    class GetAttendees {

        @Test
        @DisplayName("Возвращает список записавшихся-успехTest")
        void returnsAttendeesListTest() throws Exception {
            Lesson lesson = createFutureLesson();
            registerStudent(lesson.getId(), studentToken);
            registerStudent(lesson.getId(), otherStudentToken);

            mockMvc.perform(get("/lessons/{lessonId}/sheets", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("Возвращает 404 для несуществующего занятия-ошибкаTest")
        void returns404ForMissingLessonTest() throws Exception {
            mockMvc.perform(get("/lessons/{lessonId}/sheets", UUID.randomUUID())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken)))
                    .andExpect(status().isNotFound());
        }
    }

    // ───── PATCH /lessons/{lessonId}/sheets (отметка посещаемости) ─────

    @Nested
    @DisplayName("PATCH /lessons/{lessonId}/sheets")
    class MarkAttendance {

        @Test
        @DisplayName("Препод отмечает посещаемость на уже начавшемся занятии-успехTest")
        void teacherMarksAttendanceOnStartedLessonTest() throws Exception {
            Lesson lesson = createPastStartedLesson();
            UUID sheetId = createSheetForLesson(lesson, student);

            BulkAttendanceRequest request = new BulkAttendanceRequest(List.of(
                    new AttendanceMark(sheetId, true)
            ));

            mockMvc.perform(patch("/lessons/{lessonId}/sheets", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(teacherToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("Препод НЕ может отметить на чужом занятии-ошибкаTest")
        void teacherCannotMarkAttendanceOnAnothersLessonTest() throws Exception {
            Lesson lesson = createPastStartedLesson();
            UUID sheetId = createSheetForLesson(lesson, student);

            BulkAttendanceRequest request = new BulkAttendanceRequest(List.of(
                    new AttendanceMark(sheetId, true)
            ));

            mockMvc.perform(patch("/lessons/{lessonId}/sheets", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(otherTeacherToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Препод получает 400 при попытке отметить на ещё не начавшемся занятии-ошибкаTest")
        void teacherGets400OnNotStartedLessonTest() throws Exception {
            Lesson lesson = createFutureLesson();
            UUID sheetId = registerStudent(lesson.getId(), studentToken);

            BulkAttendanceRequest request = new BulkAttendanceRequest(List.of(
                    new AttendanceMark(sheetId, true)
            ));

            mockMvc.perform(patch("/lessons/{lessonId}/sheets", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(teacherToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]", containsString("before the lesson starts")));
        }

        @Test
        @DisplayName("Студент получает 403 на отметку посещаемости-ошибкаTest")
        void studentGets403OnMarkAttendanceTest() throws Exception {
            Lesson lesson = createPastStartedLesson();
            UUID sheetId = createSheetForLesson(lesson, student);

            BulkAttendanceRequest request = new BulkAttendanceRequest(List.of(
                    new AttendanceMark(sheetId, true)
            ));

            mockMvc.perform(patch("/lessons/{lessonId}/sheets", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ───── GET /lessons/my (мои занятия) ─────

    @Nested
    @DisplayName("GET /lessons/my")
    class MyLessons {

        @Test
        @DisplayName("Студент видит свои занятия-успехTest")
        void studentSeesHisLessonsTest() throws Exception {
            Lesson lesson = createFutureLesson();
            registerStudent(lesson.getId(), studentToken);

            mockMvc.perform(get("/lessons/my")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(lesson.getId().toString()))
                    .andExpect(jsonPath("$.content[0].sheet.id").exists());
        }

        @Test
        @DisplayName("Другой студент не видит чужие записи-успехTest")
        void otherStudentDoesNotSeeAlienRegistrationsTest() throws Exception {
            Lesson lesson = createFutureLesson();
            registerStudent(lesson.getId(), studentToken);

            mockMvc.perform(get("/lessons/my")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(otherStudentToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }

        @Test
        @DisplayName("Препод получает 403 на /lessons/my-ошибкаTest")
        void teacherGets403OnMyLessonsTest() throws Exception {
            mockMvc.perform(get("/lessons/my")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(teacherToken)))
                    .andExpect(status().isForbidden());
        }
    }

    // ───── helpers ─────

    private Lesson createFutureLesson() {
        Lesson lesson = new Lesson();
        lesson.setTitle("Тест-занятие");
        lesson.setPlace("Зал 1");
        lesson.setStartTime(OffsetDateTime.now().plusDays(1));
        lesson.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(1));
        lesson.setTotalPlaces(TOTAL_PLACES);
        lesson.setStatus(LessonStatus.ACTIVE);
        lesson.setWorkoutType(workoutType);
        lesson.setTeacher(teacher);
        lesson.setCampus(campus);
        return lessonRepository.save(lesson);
    }

    private Lesson createPastStartedLesson() {
        Lesson lesson = new Lesson();
        lesson.setTitle("Уже начавшееся занятие");
        lesson.setPlace("Зал 2");
        lesson.setStartTime(OffsetDateTime.now().minusHours(1).withOffsetSameInstant(ZoneOffset.UTC));
        lesson.setEndTime(OffsetDateTime.now().plusHours(1).withOffsetSameInstant(ZoneOffset.UTC));
        lesson.setTotalPlaces(TOTAL_PLACES);
        lesson.setStatus(LessonStatus.ACTIVE);
        lesson.setWorkoutType(workoutType);
        lesson.setTeacher(teacher);
        lesson.setCampus(campus);
        return lessonRepository.save(lesson);
    }

    private UUID createSheetForLesson(Lesson lesson, Student forStudent) {
        Sheet sheet = new Sheet();
        sheet.setLesson(lesson);
        sheet.setStudent(forStudent);
        sheet.setVisited(false);
        return sheetRepository.save(sheet).getId();
    }

    private UUID registerStudent(UUID lessonId, String token) throws Exception {
        String response = mockMvc.perform(post("/lessons/{lessonId}/sheets", lessonId)
                        .header(HttpHeaders.AUTHORIZATION, bearerHeader(token)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, SheetIdResponse.class).id();
    }
}
