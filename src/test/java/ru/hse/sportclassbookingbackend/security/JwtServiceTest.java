package ru.hse.sportclassbookingbackend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.hse.sportclassbookingbackend.model.Role;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-min-256-bytes-required-padding-to-make-it-long-enough";
    private static final Long EXPIRATION_MILLIS = 1_800_000L;
    private static final Long SHORT_EXPIRATION_MILLIS = 1L;
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION_MILLIS);
    }

    // ───── generateAccessToken ─────

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("Генерирует непустой JWT-токен в формате header.payload.signature-успехTest")
        void generatesNonEmptyJwtTokenSuccessTest() {
            String token = jwtService.generateAccessToken(USER_ID, Role.STUDENT);

            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("Генерирует разные токены для разных вызовов даже с одинаковыми входами-успехTest")
        void generatesDifferentTokensOnSubsequentCallsTest() throws InterruptedException {
            String first = jwtService.generateAccessToken(USER_ID, Role.STUDENT);
            Thread.sleep(1100);
            String second = jwtService.generateAccessToken(USER_ID, Role.STUDENT);

            assertThat(first).isNotEqualTo(second);
        }
    }

    // ───── parseToken ─────

    @Nested
    @DisplayName("parseToken")
    class ParseToken {

        @Test
        @DisplayName("Парсит валидный токен и достаёт userId+role-успехTest")
        void parsesValidTokenAndExtractsUserIdAndRoleTest() {
            String token = jwtService.generateAccessToken(USER_ID, Role.TEACHER);

            Optional<JwtService.JwtTokenData> result = jwtService.parseToken(token);

            assertThat(result).isPresent();
            assertThat(result.get().userId()).isEqualTo(USER_ID);
            assertThat(result.get().role()).isEqualTo(Role.TEACHER);
        }

        @Test
        @DisplayName("Сохраняет все три роли при round-trip-успехTest")
        void preservesAllRolesOnRoundTripTest() {
            for (Role role : Role.values()) {
                String token = jwtService.generateAccessToken(USER_ID, role);
                Optional<JwtService.JwtTokenData> result = jwtService.parseToken(token);

                assertThat(result).isPresent();
                assertThat(result.get().role()).isEqualTo(role);
            }
        }

        @Test
        @DisplayName("Возвращает Optional.empty для невалидной строки-ошибкаTest")
        void returnsEmptyForGarbageStringTest() {
            Optional<JwtService.JwtTokenData> result = jwtService.parseToken("this-is-not-a-jwt");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Возвращает Optional.empty для null-ошибкаTest")
        void returnsEmptyForNullTest() {
            Optional<JwtService.JwtTokenData> result = jwtService.parseToken(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Возвращает Optional.empty для пустой строки-ошибкаTest")
        void returnsEmptyForEmptyStringTest() {
            Optional<JwtService.JwtTokenData> result = jwtService.parseToken("");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Возвращает Optional.empty для токена подписанного чужим ключом-ошибкаTest")
        void returnsEmptyForTokenSignedWithDifferentKeyTest() {
            JwtService anotherService = new JwtService();
            ReflectionTestUtils.setField(anotherService, "secret",
                    "completely-different-secret-key-also-min-256-bytes-required-for-hs256-signature");
            ReflectionTestUtils.setField(anotherService, "expiration", EXPIRATION_MILLIS);

            String alienToken = anotherService.generateAccessToken(USER_ID, Role.ADMIN);

            Optional<JwtService.JwtTokenData> result = jwtService.parseToken(alienToken);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Возвращает Optional.empty для истёкшего токена-ошибкаTest")
        void returnsEmptyForExpiredTokenTest() throws InterruptedException {
            ReflectionTestUtils.setField(jwtService, "expiration", SHORT_EXPIRATION_MILLIS);
            String token = jwtService.generateAccessToken(USER_ID, Role.STUDENT);

            Thread.sleep(50);

            Optional<JwtService.JwtTokenData> result = jwtService.parseToken(token);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Возвращает Optional.empty для битой подписи-ошибкаTest")
        void returnsEmptyForTamperedSignatureTest() {
            String token = jwtService.generateAccessToken(USER_ID, Role.STUDENT);
            String[] parts = token.split("\\.");
            String tampered = parts[0] + "." + parts[1] + ".invalidsignature";

            Optional<JwtService.JwtTokenData> result = jwtService.parseToken(tampered);

            assertThat(result).isEmpty();
        }
    }
}