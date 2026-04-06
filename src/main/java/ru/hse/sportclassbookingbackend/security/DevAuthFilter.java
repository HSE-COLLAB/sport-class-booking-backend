package ru.hse.sportclassbookingbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.hse.sportclassbookingbackend.model.Role;

import java.io.IOException;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "security.development.default-user.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DevAuthFilter extends OncePerRequestFilter {

    @Value("${security.development.default-user.role:STUDENT}")
    private Role role;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        UserPrincipal principal = switch (role) {
            case Role.STUDENT -> new UserPrincipal(UUID.fromString("55555555-5555-5555-5555-555555555555"), role);
            case Role.TEACHER -> new UserPrincipal(UUID.fromString("33333333-3333-3333-3333-333333333333"), role);
            case Role.ADMIN -> new UserPrincipal(UUID.fromString("11111111-1111-1111-1111-111111111111"), role);
        };
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
