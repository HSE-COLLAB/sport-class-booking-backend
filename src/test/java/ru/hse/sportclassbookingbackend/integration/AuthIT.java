package ru.hse.sportclassbookingbackend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ru.hse.sportclassbookingbackend.AbstractIntegrationTest;
import ru.hse.sportclassbookingbackend.dto.auth.AuthResponse;
import ru.hse.sportclassbookingbackend.dto.auth.LoginRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterStudentRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterTeacherRequest;
import ru.hse.sportclassbookingbackend.model.StudentGroup;
import ru.hse.sportclassbookingbackend.repository.StudentGroupRepository;

import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIT extends AbstractIntegrationTest {

    private static final Integer CAMPUS_ID = 3;
    private static final Integer UNKNOWN_CAMPUS_ID = 999;
    private static final UUID UNKNOWN_GROUP_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID INVALID_REFRESH_TOKEN = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String STUDENT_EMAIL = "student@hse.ru";
    private static final String TEACHER_EMAIL = "teacher@hse.ru";
    private static final String PASSWORD = "P@ssw0rd123";
    private static final String WRONG_PASSWORD = "wrongPassword";

    @Autowired private ObjectMapper objectMapper;
    @Autowired private StudentGroupRepository studentGroupRepository;

    private UUID studentGroupId;

    @BeforeEach
    void createStudentGroup() {
        StudentGroup group = new StudentGroup();
        group.setFaculty("ФКН");
        group.setAcademicMajor("Программная инженерия");
        group.setGroupNumber("ПИ-2024");
        studentGroupId = studentGroupRepository.save(group).getId();
    }

    // ───── registerStudent ─────

    @Nested
    @DisplayName("POST /auth/register/student")
    class RegisterStudent {

        @Test
        @DisplayName("Создаёт студента и возвращает 201 с парой токенов-успехTest")
        void createsStudentAndReturns201WithTokensTest() throws Exception {
            RegisterStudentRequest request = new RegisterStudentRequest(
                    "Иван", "Петров", "Сергеевич",
                    STUDENT_EMAIL, PASSWORD, studentGroupId, CAMPUS_ID
            );

            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken", notNullValue()))
                    .andExpect(jsonPath("$.refreshToken", notNullValue()));
        }

        @Test
        @DisplayName("Возвращает 409 при попытке зарегистрироваться на занятый email-ошибкаTest")
        void returns409OnDuplicateEmailTest() throws Exception {
            RegisterStudentRequest request = new RegisterStudentRequest(
                    "Иван", "Петров", "Сергеевич",
                    STUDENT_EMAIL, PASSWORD, studentGroupId, CAMPUS_ID
            );

            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString(STUDENT_EMAIL)));
        }

        @Test
        @DisplayName("Возвращает 400 если groupId не существует-ошибкаTest")
        void returns400WhenStudentGroupMissingTest() throws Exception {
            RegisterStudentRequest request = new RegisterStudentRequest(
                    "Иван", "Петров", "Сергеевич",
                    STUDENT_EMAIL, PASSWORD, UNKNOWN_GROUP_ID, CAMPUS_ID
            );

            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("Student group")));
        }

        @Test
        @DisplayName("Возвращает 400 если campusId не существует-ошибкаTest")
        void returns400WhenCampusMissingTest() throws Exception {
            RegisterStudentRequest request = new RegisterStudentRequest(
                    "Иван", "Петров", "Сергеевич",
                    STUDENT_EMAIL, PASSWORD, studentGroupId, UNKNOWN_CAMPUS_ID
            );

            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Возвращает 400 при невалидном email-ошибкаTest")
        void returns400OnInvalidEmailTest() throws Exception {
            RegisterStudentRequest request = new RegisterStudentRequest(
                    "Иван", "Петров", "Сергеевич",
                    "not-an-email", PASSWORD, studentGroupId, CAMPUS_ID
            );

            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ───── registerTeacher ─────

    @Nested
    @DisplayName("POST /auth/register/teacher")
    class RegisterTeacher {

        @Test
        @DisplayName("Создаёт препода и возвращает 201 с парой токенов-успехTest")
        void createsTeacherAndReturns201WithTokensTest() throws Exception {
            RegisterTeacherRequest request = new RegisterTeacherRequest(
                    "Анна", "Смирнова", "Игоревна",
                    TEACHER_EMAIL, PASSWORD, "Доцент", CAMPUS_ID
            );

            mockMvc.perform(post("/auth/register/teacher")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken", notNullValue()))
                    .andExpect(jsonPath("$.refreshToken", notNullValue()));
        }
    }

    // ───── login ─────

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @BeforeEach
        void registerStudent() throws Exception {
            RegisterStudentRequest request = new RegisterStudentRequest(
                    "Иван", "Петров", "Сергеевич",
                    STUDENT_EMAIL, PASSWORD, studentGroupId, CAMPUS_ID
            );
            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Логин с корректными кредами возвращает 200 с токенами-успехTest")
        void loginsSuccessfullyWithCorrectCredentialsTest() throws Exception {
            LoginRequest request = new LoginRequest(STUDENT_EMAIL, PASSWORD);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", notNullValue()))
                    .andExpect(jsonPath("$.refreshToken", notNullValue()));
        }

        @Test
        @DisplayName("Возвращает 401 при неверном пароле-ошибкаTest")
        void returns401OnWrongPasswordTest() throws Exception {
            LoginRequest request = new LoginRequest(STUDENT_EMAIL, WRONG_PASSWORD);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Возвращает 401 для несуществующего email-ошибкаTest")
        void returns401ForUnknownEmailTest() throws Exception {
            LoginRequest request = new LoginRequest("nobody@hse.ru", PASSWORD);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ───── refresh + logout (full flow) ─────

    @Nested
    @DisplayName("Полный auth-flow: register → login → refresh → logout")
    class FullFlow {

        @Test
        @DisplayName("Сквозной сценарий register → login → refresh → logout → refresh=401-успехTest")
        void fullAuthFlowSuccessTest() throws Exception {
            // 1. Регистрация
            RegisterStudentRequest registerRequest = new RegisterStudentRequest(
                    "Иван", "Петров", "Сергеевич",
                    STUDENT_EMAIL, PASSWORD, studentGroupId, CAMPUS_ID
            );
            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated());

            // 2. Логин
            LoginRequest loginRequest = new LoginRequest(STUDENT_EMAIL, PASSWORD);
            String loginResponse = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            AuthResponse tokens = objectMapper.readValue(loginResponse, AuthResponse.class);
            UUID refreshToken = tokens.refreshToken();

            // 3. Refresh — выдаёт новый access с тем же refresh
            String refreshResponse = mockMvc.perform(post("/auth/refresh")
                            .header("X-Refresh-Token", refreshToken.toString()))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            AuthResponse refreshed = objectMapper.readValue(refreshResponse, AuthResponse.class);
            org.assertj.core.api.Assertions.assertThat(refreshed.refreshToken()).isEqualTo(refreshToken);
            org.assertj.core.api.Assertions.assertThat(refreshed.accessToken()).isNotBlank();

            // 4. Logout
            mockMvc.perform(post("/auth/logout")
                            .header("X-Refresh-Token", refreshToken.toString()))
                    .andExpect(status().isNoContent());

            // 5. Refresh после logout — 401
            mockMvc.perform(post("/auth/refresh")
                            .header("X-Refresh-Token", refreshToken.toString()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Refresh с неизвестным токеном возвращает 401-ошибкаTest")
        void returns401OnUnknownRefreshTokenTest() throws Exception {
            mockMvc.perform(post("/auth/refresh")
                            .header("X-Refresh-Token", INVALID_REFRESH_TOKEN.toString()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Logout идемпотентен — повторный logout тоже 204-успехTest")
        void logoutIsIdempotentTest() throws Exception {
            mockMvc.perform(post("/auth/logout")
                            .header("X-Refresh-Token", INVALID_REFRESH_TOKEN.toString()))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/auth/logout")
                            .header("X-Refresh-Token", INVALID_REFRESH_TOKEN.toString()))
                    .andExpect(status().isNoContent());
        }
    }
}