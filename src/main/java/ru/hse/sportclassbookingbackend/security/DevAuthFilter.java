package ru.hse.sportclassbookingbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.hse.sportclassbookingbackend.model.User;
import ru.hse.sportclassbookingbackend.repository.UserRepository;

import java.io.IOException;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "security.development.default-user.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DevAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Dev-User-Id";

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String headerValue = request.getHeader(HEADER_NAME);
        if (headerValue != null && !headerValue.isBlank()) {
            try {
                UUID userId = UUID.fromString(headerValue.trim());
                userRepository.findById(userId).ifPresent(user -> {
                    UserPrincipal principal = new UserPrincipal(user.getId(), user.getRole());
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
                    );
                });
            } catch (IllegalArgumentException ignored) {
                // invalid UUID — request proceeds without authentication
            }
        }
        filterChain.doFilter(request, response);
    }
}
