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
import ru.hse.sportclassbookingbackend.security.JwtAuthenticationFilter;

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

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        authorizeHttp -> {
                            authorizeHttp.requestMatchers("/auth/**", "/error", "/actuator").permitAll()
                                    .anyRequest().authenticated();
                        }
                )
                .addFilterBefore(jwtAuthenticationFilter, AuthorizationFilter.class);
        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptRounds);
    }
}
