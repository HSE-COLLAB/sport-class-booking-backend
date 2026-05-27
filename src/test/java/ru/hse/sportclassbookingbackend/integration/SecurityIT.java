package ru.hse.sportclassbookingbackend.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import ru.hse.sportclassbookingbackend.AbstractIntegrationTest;
import ru.hse.sportclassbookingbackend.model.Role;
import ru.hse.sportclassbookingbackend.security.JwtService;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIT extends AbstractIntegrationTest {

    private static final String PROTECTED_ENDPOINT = "/lessons";
    private static final Integer ANY_CAMPUS_ID = 1;
    private static final UUID ANY_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String JWT_SECRET = "very-very-secret-key-min-256-bytes-required";

    @Test
    @DisplayName("Защищённый эндпоинт без Authorization-заголовка возвращает 403-ошибкаTest")
    void protectedEndpointReturns403WithoutAuthHeaderTest() throws Exception {
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                        .param("campusId", String.valueOf(ANY_CAMPUS_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Битый JWT (рандомная строка) возвращает 401-ошибкаTest")
    void garbageTokenReturns401Test() throws Exception {
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                        .param("campusId", String.valueOf(ANY_CAMPUS_ID))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0]", containsString("Invalid or expired token")));
    }

    @Test
    @DisplayName("JWT с подделанной подписью возвращает 401-ошибкаTest")
    void tamperedSignatureReturns401Test() throws Exception {
        String validToken = generateValidToken();
        String[] parts = validToken.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".thisIsNotARealSignature";

        mockMvc.perform(get(PROTECTED_ENDPOINT)
                        .param("campusId", String.valueOf(ANY_CAMPUS_ID))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0]", containsString("Invalid or expired token")));
    }

    @Test
    @DisplayName("Истёкший JWT возвращает 401-ошибкаTest")
    void expiredTokenReturns401Test() throws Exception {
        String expiredToken = generateExpiredToken();

        mockMvc.perform(get(PROTECTED_ENDPOINT)
                        .param("campusId", String.valueOf(ANY_CAMPUS_ID))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0]", containsString("Invalid or expired token")));
    }

    @Test
    @DisplayName("JWT подписанный чужим секретом возвращает 401-ошибкаTest")
    void tokenSignedWithAlienSecretReturns401Test() throws Exception {
        String alienToken = generateTokenWithSecret(
        );

        mockMvc.perform(get(PROTECTED_ENDPOINT)
                        .param("campusId", String.valueOf(ANY_CAMPUS_ID))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + alienToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0]", containsString("Invalid or expired token")));
    }

    // ───── helpers ─────

    private String generateValidToken() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", JWT_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 1_800_000L);
        return jwtService.generateAccessToken(ANY_USER_ID, Role.STUDENT);
    }

    private String generateExpiredToken() throws Exception {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", JWT_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 1L);
        String token = jwtService.generateAccessToken(ANY_USER_ID, Role.STUDENT);
        Thread.sleep(50);
        return token;
    }

    private String generateTokenWithSecret() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "completely-different-secret-key-also-min-256-bytes-required-for-hs256-signature");
        ReflectionTestUtils.setField(jwtService, "expiration", 1_800_000L);
        return jwtService.generateAccessToken(ANY_USER_ID, Role.STUDENT);
    }
}