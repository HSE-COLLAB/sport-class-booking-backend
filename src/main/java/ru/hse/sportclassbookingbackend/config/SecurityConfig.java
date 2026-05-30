package ru.hse.sportclassbookingbackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import ru.hse.sportclassbookingbackend.security.DevAuthFilter;
import ru.hse.sportclassbookingbackend.security.JwtAuthenticationFilter;
import ru.hse.sportclassbookingbackend.security.RestAccessDeniedHandler;
import ru.hse.sportclassbookingbackend.security.RestAuthenticationEntryPoint;
import ru.hse.sportclassbookingbackend.web.HttpLoggingFilter;

import java.util.Optional;

/**
    Заголовок Authorization игнорируется при обращении к /api/auth/**

    Возможные ошибки в response:

    Установлен заголовок Authorization и аутентификация успешна -> любая ошибка 4** (кроме 401), 5** (в т.ч. 403 если нету прав)
    Установлен заголовок Authorization, но не удается аутентифицировать пользователя -> всегда 401/500

    Отсутствует заголовок Authorization и эндпоинт публичный -> любая ошибка
    Отсутствует заголовок Authorization и эндпоинта нету в списке публичных -> всегда 403/500
 */

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${security.bcrypt.rounds}")
    private Integer bcryptRounds;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final Optional<DevAuthFilter> devAuthFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        authorizeHttp -> {
                            authorizeHttp.requestMatchers(
                                    "/auth/**",
                                    "/error",
                                    "/actuator/**",
                                    "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml",
                                    "/swagger-ui.html", "/swagger-ui/**"
                            ).permitAll()
                                    .anyRequest().authenticated();
                        }
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, AuthorizationFilter.class)
                .addFilterBefore(new HttpLoggingFilter(), JwtAuthenticationFilter.class);

        devAuthFilter.ifPresent(f ->
                http.addFilterBefore(f, JwtAuthenticationFilter.class)
        );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptRounds);
    }
}
