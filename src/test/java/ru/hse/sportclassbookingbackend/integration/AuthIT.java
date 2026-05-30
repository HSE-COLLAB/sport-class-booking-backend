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
import ru.hse.sportclassbookingbackend.dto.auth.ResendVerificationRequest;
import ru.hse.sportclassbookingbackend.model.StudentGroup;
import ru.hse.sportclassbookingbackend.model.User;
import ru.hse.sportclassbookingbackend.repository.StudentGroupRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIT extends AbstractIntegrationTest {

    private static final Integer CAMPUS_ID = 3;
    private static final Integer UNKNOWN_CAMPUS_ID = 999;
    private static final UUID UNKNOWN_GROUP_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID INVALID_REFRESH_TOKEN = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INVALID_VERIFY_TOKEN = UUID.fromString("22222222-2222-2222-2222-222222222222");

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
        @DisplayName("Создаёт unverified-студента и возвращает 202 без токенов-успехTest")
        void createsUnverifiedStudentAndReturns202Test() throws Exception {
            RegisterStudentRequest request = studentRequest(STUDENT_EMAIL);

            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.email", equalTo(STUDENT_EMAIL)))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.accessToken").doesNotExist())
                    .andExpect(jsonPath("$.refreshToken").doesNotExist());

            User saved = userRepository.findByEmail(STUDENT_EMAIL).orElseThrow();
            assertThat(saved.getEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("Повторная регистрация на тот же email c unverified юзером — заменяет старого-успехTest")
        void replacesUnverifiedUserOnRepeatedRegistrationTest() throws Exception {
            RegisterStudentRequest first = studentRequest(STUDENT_EMAIL);
            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(first)))
                    .andExpect(status().isAccepted());
            UUID firstUserId = userRepository.findByEmail(STUDENT_EMAIL).orElseThrow().getId();

            RegisterStudentRequest second = studentRequest(STUDENT_EMAIL);
            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(second)))
                    .andExpect(status().isAccepted());

            UUID secondUserId = userRepository.findByEmail(STUDENT_EMAIL).orElseThrow().getId();
            assertThat(secondUserId).isNotEqualTo(firstUserId);
        }

        @Test
        @DisplayName("Возвращает 409 при попытке зарегистрироваться на уже подтверждённый email-ошибкаTest")
        void returns409OnVerifiedEmailTest() throws Exception {
            RegisterStudentRequest request = studentRequest(STUDENT_EMAIL);
            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());
            verifyEmailFor(STUDENT_EMAIL);

            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString(STUDENT_EMAIL)));
        }

        @Test
        @DisplayName("Возвращает 404 если groupId не существует-ошибкаTest")
        void returns404WhenStudentGroupMissingTest() throws Exception {
            // Сервис кидает NotFoundException (→ 404), не BadRequestException (→ 400).
            // Это поведение унаследовано из develop, не связано с email-интеграцией.
            RegisterStudentRequest request = new RegisterStudentRequest(
                    "Иван", "Петров", "Сергеевич",
                    STUDENT_EMAIL, PASSWORD, UNKNOWN_GROUP_ID, CAMPUS_ID
            );

            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("Student group")));
        }

        @Test
        @DisplayName("Возвращает 404 если campusId не существует-ошибкаTest")
        void returns404WhenCampusMissingTest() throws Exception {
            RegisterStudentRequest request = new RegisterStudentRequest(
                    "Иван", "Петров", "Сергеевич",
                    STUDENT_EMAIL, PASSWORD, studentGroupId, UNKNOWN_CAMPUS_ID
            );

            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
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
        @DisplayName("Создаёт unverified-препода и возвращает 202 без токенов-успехTest")
        void createsUnverifiedTeacherAndReturns202Test() throws Exception {
            RegisterTeacherRequest request = teacherRequest();

            mockMvc.perform(post("/auth/register/teacher")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.email", equalTo(TEACHER_EMAIL)))
                    .andExpect(jsonPath("$.accessToken").doesNotExist());

            assertThat(userRepository.findByEmail(TEACHER_EMAIL).orElseThrow().getEmailVerified())
                    .isFalse();
        }
    }

    // ───── verify-email ─────

    @Nested
    @DisplayName("GET /auth/verify-email")
    class VerifyEmail {

        @Test
        @DisplayName("По валидному токену помечает юзера verified и возвращает 204-успехTest")
        void marksUserVerifiedTest() throws Exception {
            RegisterStudentRequest request = studentRequest(STUDENT_EMAIL);
            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());
            assertThat(userRepository.findByEmail(STUDENT_EMAIL).orElseThrow().getEmailVerified())
                    .isFalse();

            verifyEmailFor(STUDENT_EMAIL);

            assertThat(userRepository.findByEmail(STUDENT_EMAIL).orElseThrow().getEmailVerified())
                    .isTrue();
        }

        @Test
        @DisplayName("По неизвестному токену возвращает 400-ошибкаTest")
        void returns400OnUnknownTokenTest() throws Exception {
            mockMvc.perform(get("/auth/verify-email").param("token", INVALID_VERIFY_TOKEN.toString()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Повторное использование того же токена возвращает 400-ошибкаTest")
        void returns400OnSecondUseTest() throws Exception {
            RegisterStudentRequest request = studentRequest(STUDENT_EMAIL);
            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());

            UUID userId = userRepository.findByEmail(STUDENT_EMAIL).orElseThrow().getId();
            UUID token = emailVerificationTokenService.create(userId);

            mockMvc.perform(get("/auth/verify-email").param("token", token.toString()))
                    .andExpect(status().isNoContent());
            mockMvc.perform(get("/auth/verify-email").param("token", token.toString()))
                    .andExpect(status().isBadRequest());
        }
    }

    // ───── resend-verification ─────

    @Nested
    @DisplayName("POST /auth/resend-verification")
    class ResendVerification {

        @Test
        @DisplayName("Для unverified юзера возвращает 204-успехTest")
        void returns204ForUnverifiedTest() throws Exception {
            RegisterStudentRequest request = studentRequest(STUDENT_EMAIL);
            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());

            ResendVerificationRequest resend = new ResendVerificationRequest(STUDENT_EMAIL);
            mockMvc.perform(post("/auth/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resend)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Для unknown email тоже возвращает 204 (silent, не светим существование)-успехTest")
        void returns204ForUnknownEmailTest() throws Exception {
            ResendVerificationRequest resend = new ResendVerificationRequest("nobody@hse.ru");
            mockMvc.perform(post("/auth/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resend)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Возвращает 400 при невалидном email в теле-ошибкаTest")
        void returns400OnInvalidEmailTest() throws Exception {
            ResendVerificationRequest resend = new ResendVerificationRequest("not-an-email");
            mockMvc.perform(post("/auth/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resend)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ───── login ─────

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @BeforeEach
        void registerStudent() throws Exception {
            RegisterStudentRequest request = studentRequest(STUDENT_EMAIL);
            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("Логин unverified юзера возвращает 401-ошибкаTest")
        void returns401ForUnverifiedTest() throws Exception {
            LoginRequest request = new LoginRequest(STUDENT_EMAIL, PASSWORD);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Логин verified юзера возвращает 200 с токенами-успехTest")
        void loginsSuccessfullyAfterVerifyTest() throws Exception {
            verifyEmailFor(STUDENT_EMAIL);
            LoginRequest request = new LoginRequest(STUDENT_EMAIL, PASSWORD);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists());
        }

        @Test
        @DisplayName("Возвращает 401 при неверном пароле-ошибкаTest")
        void returns401OnWrongPasswordTest() throws Exception {
            verifyEmailFor(STUDENT_EMAIL);
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
    @DisplayName("Полный auth-flow: register → verify → login → refresh → logout")
    class FullFlow {

        @Test
        @DisplayName("Сквозной сценарий с verify-успехTest")
        void fullAuthFlowSuccessTest() throws Exception {
            // 1. Регистрация — 202, без токенов
            RegisterStudentRequest registerRequest = studentRequest(STUDENT_EMAIL);
            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isAccepted());

            // 2. Verify email
            verifyEmailFor(STUDENT_EMAIL);

            // 3. Логин
            LoginRequest loginRequest = new LoginRequest(STUDENT_EMAIL, PASSWORD);
            String loginResponse = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            AuthResponse tokens = objectMapper.readValue(loginResponse, AuthResponse.class);
            UUID refreshToken = tokens.refreshToken();

            // 4. Refresh
            String refreshResponse = mockMvc.perform(post("/auth/refresh")
                            .header("X-Refresh-Token", refreshToken.toString()))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            AuthResponse refreshed = objectMapper.readValue(refreshResponse, AuthResponse.class);
            assertThat(refreshed.refreshToken()).isEqualTo(refreshToken);
            assertThat(refreshed.accessToken()).isNotBlank();

            // 5. Logout
            mockMvc.perform(post("/auth/logout")
                            .header("X-Refresh-Token", refreshToken.toString()))
                    .andExpect(status().isNoContent());

            // 6. Refresh после logout — 401
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

    // ───── forgotPassword + resetPassword ─────

    @Nested
    @DisplayName("Password reset flow")
    class PasswordReset {

        @BeforeEach
        void registerAndVerify() throws Exception {
            RegisterStudentRequest req = studentRequest(STUDENT_EMAIL);
            mockMvc.perform(post("/auth/register/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isAccepted());
            verifyEmailFor(STUDENT_EMAIL);
        }

        @Test
        @DisplayName("forgot → reset → login со старым паролем 401, с новым 200-успехTest")
        void fullPasswordResetFlowTest() throws Exception {
            // 1. Forgot
            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + STUDENT_EMAIL + "\"}"))
                    .andExpect(status().isNoContent());

            String code = readPasswordResetCode(STUDENT_EMAIL);
            org.assertj.core.api.Assertions.assertThat(code).matches("\\d{6}");

            // 2. Reset
            String newPassword = "N3wP@ss123";
            String resetBody = "{\"email\":\"" + STUDENT_EMAIL + "\",\"code\":\"" + code + "\",\"newPassword\":\"" + newPassword + "\"}";
            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetBody))
                    .andExpect(status().isNoContent());

            // 3. Старый пароль → 401
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + STUDENT_EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                    .andExpect(status().isUnauthorized());

            // 4. Новый пароль → 200
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + STUDENT_EMAIL + "\",\"password\":\"" + newPassword + "\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("forgot на неизвестный email → 204 (silent)-успехTest")
        void forgotSilentForUnknownEmailTest() throws Exception {
            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"nobody-totally-random@hse.ru\"}"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("reset с неверным PIN → 400-ошибкаTest")
        void resetWithWrongCodeTest() throws Exception {
            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + STUDENT_EMAIL + "\"}"))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + STUDENT_EMAIL + "\",\"code\":\"000000\",\"newPassword\":\"whatever\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("reset с невалидным форматом code (не 6 цифр) → 400 от валидатора-ошибкаTest")
        void resetWithMalformedCodeTest() throws Exception {
            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + STUDENT_EMAIL + "\",\"code\":\"abc\",\"newPassword\":\"whatever\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    private RegisterStudentRequest studentRequest(String email) {
        return new RegisterStudentRequest(
                "Иван", "Петров", "Сергеевич",
                email, PASSWORD, studentGroupId, CAMPUS_ID
        );
    }

    private RegisterTeacherRequest teacherRequest() {
        return new RegisterTeacherRequest(
                "Анна", "Смирнова", "Игоревна",
                TEACHER_EMAIL, PASSWORD, "Доцент", CAMPUS_ID
        );
    }
}
