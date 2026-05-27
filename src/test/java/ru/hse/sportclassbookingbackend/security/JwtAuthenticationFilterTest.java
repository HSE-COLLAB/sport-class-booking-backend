package ru.hse.sportclassbookingbackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.hse.sportclassbookingbackend.model.Role;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String INVALID_TOKEN = "invalid.jwt.token";
    private static final String BEARER_PREFIX = "Bearer ";

    @Mock private JwtService jwtService;
    @Mock private FilterChain filterChain;

    private ObjectMapper objectMapper;
    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        filter = new JwtAuthenticationFilter(jwtService, objectMapper);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ───── doFilterInternal ─────

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        @Test
        @DisplayName("Пропускает запрос без Authorization-заголовка без аутентификации-успехTest")
        void passesRequestWithoutAuthHeaderWithoutAuthenticationTest() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).parseToken(any());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Пропускает запрос с не-Bearer заголовком без аутентификации-успехTest")
        void passesRequestWithNonBearerHeaderWithoutAuthenticationTest() throws Exception {
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).parseToken(any());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Кладёт UserPrincipal в SecurityContext при валидном токене-успехTest")
        void setsUserPrincipalInSecurityContextOnValidTokenTest() throws Exception {
            request.addHeader("Authorization", BEARER_PREFIX + VALID_TOKEN);
            when(jwtService.parseToken(VALID_TOKEN))
                    .thenReturn(Optional.of(new JwtService.JwtTokenData(USER_ID, Role.TEACHER)));

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);

            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            assertThat(principal.getId()).isEqualTo(USER_ID);
            assertThat(principal.getRole()).isEqualTo(Role.TEACHER);
            assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_TEACHER");

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Отдаёт 401 с JSON-телом при невалидном токене-ошибкаTest")
        void returns401WithJsonBodyOnInvalidTokenTest() throws Exception {
            request.addHeader("Authorization", BEARER_PREFIX + INVALID_TOKEN);
            when(jwtService.parseToken(INVALID_TOKEN)).thenReturn(Optional.empty());

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentType()).startsWith("application/json");
            assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
            assertThat(response.getContentAsString())
                    .contains("Invalid or expired token");

            verify(filterChain, never()).doFilter(any(), any());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Извлекает токен после префикса 'Bearer '-успехTest")
        void extractsTokenAfterBearerPrefixTest() throws Exception {
            request.addHeader("Authorization", BEARER_PREFIX + VALID_TOKEN);
            when(jwtService.parseToken(VALID_TOKEN))
                    .thenReturn(Optional.of(new JwtService.JwtTokenData(USER_ID, Role.STUDENT)));

            filter.doFilterInternal(request, response, filterChain);

            verify(jwtService).parseToken(VALID_TOKEN);
        }
    }

    // ───── shouldNotFilter ─────

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilter {

        @Test
        @DisplayName("Пропускает фильтрацию для /auth/login-успехTest")
        void skipsFilterForAuthLoginTest() {
            request.setServletPath("/auth/login");

            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("Пропускает фильтрацию для /auth/refresh-успехTest")
        void skipsFilterForAuthRefreshTest() {
            request.setServletPath("/auth/refresh");

            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("Пропускает фильтрацию для /v3/api-docs-успехTest")
        void skipsFilterForOpenApiDocsTest() {
            request.setServletPath("/v3/api-docs");

            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("Пропускает фильтрацию для /v3/api-docs/swagger-config-успехTest")
        void skipsFilterForOpenApiDocsSubpathTest() {
            request.setServletPath("/v3/api-docs/swagger-config");

            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("Пропускает фильтрацию для /swagger-ui/index.html-успехTest")
        void skipsFilterForSwaggerUiSubpathTest() {
            request.setServletPath("/swagger-ui/index.html");

            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("НЕ пропускает фильтрацию для защищённых эндпоинтов-успехTest")
        void doesNotSkipFilterForProtectedEndpointsTest() {
            request.setServletPath("/lessons");

            assertThat(filter.shouldNotFilter(request)).isFalse();
        }

        @Test
        @DisplayName("НЕ пропускает фильтрацию для /students-успехTest")
        void doesNotSkipFilterForStudentsEndpointTest() {
            request.setServletPath("/students");

            assertThat(filter.shouldNotFilter(request)).isFalse();
        }
    }
}
