package ru.hse.sportclassbookingbackend.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import ru.hse.sportclassbookingbackend.AbstractIntegrationTest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonPatchRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonStatus;
import ru.hse.sportclassbookingbackend.model.Campus;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.model.Lesson;
import ru.hse.sportclassbookingbackend.model.StudentGroup;
import ru.hse.sportclassbookingbackend.model.Teacher;
import ru.hse.sportclassbookingbackend.model.WorkoutType;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LessonIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@hse.ru";
    private static final String TEACHER_EMAIL = "teacher@hse.ru";
    private static final String OTHER_TEACHER_EMAIL = "other.teacher@hse.ru";
    private static final String STUDENT_EMAIL = "student@hse.ru";

    private static final LocalDateTime LESSON_START = LocalDateTime.of(2099, 6, 1, 10, 0);
    private static final LocalDateTime LESSON_END = LocalDateTime.of(2099, 6, 1, 11, 0);
    private static final int TOTAL_PLACES = 15;

    private WorkoutType workoutType;
    private Teacher teacher;
    private Teacher otherTeacher;

    private String adminToken;
    private String teacherToken;
    private String otherTeacherToken;
    private String studentToken;

    @BeforeEach
    void setUpFixtures() throws Exception {
        Campus campus = campusRepository.findById(NN_CAMPUS_ID).orElseThrow();
        HealthGroup healthGroup = healthGroupRepository.findById(DEFAULT_HEALTH_GROUP_ID).orElseThrow();
        StudentGroup studentGroup = createDefaultStudentGroup();

        workoutType = new WorkoutType();
        workoutType.setTitle("Волейбол");
        workoutType.setIsActive(true);
        workoutType.setAllowedHealthGroups(Set.of(healthGroup));
        workoutType = workoutTypeRepository.save(workoutType);

        createAdmin(ADMIN_EMAIL);
        teacher = createTeacher(TEACHER_EMAIL, campus);
        otherTeacher = createTeacher(OTHER_TEACHER_EMAIL, campus);
        createStudent(STUDENT_EMAIL, campus, healthGroup, studentGroup);

        adminToken = loginAndGetToken(ADMIN_EMAIL);
        teacherToken = loginAndGetToken(TEACHER_EMAIL);
        otherTeacherToken = loginAndGetToken(OTHER_TEACHER_EMAIL);
        studentToken = loginAndGetToken(STUDENT_EMAIL);
    }

    // ───── POST /lessons ─────

    @Nested
    @DisplayName("POST /lessons")
    class Create {

        @Test
        @DisplayName("Админ создаёт занятие с явным teacherId-успехTest")
        void adminCreatesLessonWithExplicitTeacherIdTest() throws Exception {
            LessonRequest request = lessonRequest(teacher.getId());

            mockMvc.perform(post("/lessons")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.title").value("Тест-занятие"))
                    .andExpect(jsonPath("$.status").value(LessonStatus.ACTIVE.name()))
                    .andExpect(jsonPath("$.teacher.id").value(teacher.getId().toString()));
        }

        @Test
        @DisplayName("Препод создаёт занятие для себя-успехTest")
        void teacherCreatesLessonForHimselfTest() throws Exception {
            LessonRequest request = lessonRequest(teacher.getId());

            mockMvc.perform(post("/lessons")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(teacherToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.teacher.id").value(teacher.getId().toString()));
        }

        @Test
        @DisplayName("Студент получает 403 при попытке создать занятие-ошибкаTest")
        void studentGets403OnCreateTest() throws Exception {
            LessonRequest request = lessonRequest(teacher.getId());

            mockMvc.perform(post("/lessons")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Без токена возвращает 401-ошибкаTest")
        void returns401WithoutTokenTest() throws Exception {
            LessonRequest request = lessonRequest(teacher.getId());

            mockMvc.perform(post("/lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Препод НЕ может создать занятие на чужого препода-ошибкаTest")
        void teacherCannotCreateLessonForAnotherTeacherTest() throws Exception {
            LessonRequest request = lessonRequest(otherTeacher.getId());

            mockMvc.perform(post("/lessons")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(teacherToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors[0]", containsString("another teacher")));
        }

        @Test
        @DisplayName("Админ без teacherId получает 400-ошибкаTest")
        void adminWithoutTeacherIdGets400Test() throws Exception {
            LessonRequest request = lessonRequest(null);

            mockMvc.perform(post("/lessons")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]", containsString("teacherId")));
        }

        @Test
        @DisplayName("Конфликт при пересечении времени у препода возвращает 409-ошибкаTest")
        void returns409OnTimeOverlapTest() throws Exception {
            LessonRequest request = lessonRequest(teacher.getId());

            mockMvc.perform(post("/lessons")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/lessons")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    // ───── GET /lessons ─────

    @Nested
    @DisplayName("GET /lessons")
    class GetAll {

        @Test
        @DisplayName("Возвращает список занятий с фильтром по кампусу-успехTest")
        void returnsLessonsFilteredByCampusTest() throws Exception {
            createLessonInDb();

            mockMvc.perform(get("/lessons")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken))
                            .param("campusId", String.valueOf(NN_CAMPUS_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        @DisplayName("Без обязательного campusId возвращает 400-ошибкаTest")
        void returns400WithoutCampusIdTest() throws Exception {
            mockMvc.perform(get("/lessons")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ───── GET /lessons/{id} ─────

    @Nested
    @DisplayName("GET /lessons/{id}")
    class GetById {

        @Test
        @DisplayName("Возвращает занятие по id-успехTest")
        void returnsLessonByIdTest() throws Exception {
            Lesson lesson = createLessonInDb();

            mockMvc.perform(get("/lessons/{id}", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(lesson.getId().toString()));
        }

        @Test
        @DisplayName("Возвращает 404 если занятие не найдено-ошибкаTest")
        void returns404WhenLessonMissingTest() throws Exception {
            mockMvc.perform(get("/lessons/{id}", UUID.randomUUID())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken)))
                    .andExpect(status().isNotFound());
        }
    }

    // ───── PATCH /lessons/{id} ─────

    @Nested
    @DisplayName("PATCH /lessons/{id}")
    class Update {

        @Test
        @DisplayName("Админ обновляет название занятия-успехTest")
        void adminUpdatesTitleTest() throws Exception {
            Lesson lesson = createLessonInDb();
            LessonPatchRequest request = new LessonPatchRequest(
                    "Обновлённый заголовок", null, null, null, null, null, null, null, null
            );

            mockMvc.perform(patch("/lessons/{id}", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Обновлённый заголовок"));
        }

        @Test
        @DisplayName("Препод НЕ может править чужое занятие-ошибкаTest")
        void teacherCannotPatchAnothersLessonTest() throws Exception {
            Lesson lesson = createLessonInDb();
            LessonPatchRequest request = new LessonPatchRequest(
                    "Не должно сработать", null, null, null, null, null, null, null, null
            );

            mockMvc.perform(patch("/lessons/{id}", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(otherTeacherToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Студент получает 403 на PATCH-ошибкаTest")
        void studentGets403OnPatchTest() throws Exception {
            Lesson lesson = createLessonInDb();
            LessonPatchRequest request = new LessonPatchRequest(
                    "Не должно сработать", null, null, null, null, null, null, null, null
            );

            mockMvc.perform(patch("/lessons/{id}", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ───── POST /lessons/{id}/cancel ─────

    @Nested
    @DisplayName("POST /lessons/{id}/cancel")
    class Cancel {

        @Test
        @DisplayName("Админ отменяет занятие-успехTest")
        void adminCancelsLessonTest() throws Exception {
            Lesson lesson = createLessonInDb();

            mockMvc.perform(post("/lessons/{id}/cancel", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(LessonStatus.CANCELLED.name()));
        }

        @Test
        @DisplayName("Повторная отмена возвращает 409-ошибкаTest")
        void doubleCancelReturns409Test() throws Exception {
            Lesson lesson = createLessonInDb();

            mockMvc.perform(post("/lessons/{id}/cancel", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(adminToken)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/lessons/{id}/cancel", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(adminToken)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Студент получает 403 на cancel-ошибкаTest")
        void studentGets403OnCancelTest() throws Exception {
            Lesson lesson = createLessonInDb();

            mockMvc.perform(post("/lessons/{id}/cancel", lesson.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader(studentToken)))
                    .andExpect(status().isForbidden());
        }
    }

    // ───── helpers ─────

    private LessonRequest lessonRequest(UUID teacherId) {
        return new LessonRequest(
                "Тест-занятие", "Зал 1", LESSON_START, LESSON_END, TOTAL_PLACES,
                workoutType.getId(), NN_CAMPUS_ID, teacherId, null
        );
    }

    private Lesson createLessonInDb() throws Exception {
        LessonRequest request = lessonRequest(teacher.getId());
        String response = mockMvc.perform(post("/lessons")
                        .header(HttpHeaders.AUTHORIZATION, bearerHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        return lessonRepository.findById(id).orElseThrow();
    }
}