package ru.hse.sportclassbookingbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.hse.sportclassbookingbackend.model.Role;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${security.development.enabled:false}")
    private Boolean devModEnabled;

    @Value("${security.development.role:STUDENT}")
    private Role role;

    public final JwtService jwtService;

    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    private static final List<String> SKIP_PATH = List.of("/auth/**");


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (setDefaultUserForDevelopment()){
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7);
        Optional<JwtService.JwtTokenData> dataOpt = jwtService.parseToken(token);

        if (dataOpt.isPresent()) {
            JwtService.JwtTokenData data = dataOpt.get();
            UserPrincipal principal = new UserPrincipal(data.userId(), data.role());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return SKIP_PATH.stream().
                anyMatch(pattern -> antPathMatcher.match(pattern, request.getRequestURI()));
    }

    private boolean setDefaultUserForDevelopment() {
        if (!devModEnabled) return false;

        UserPrincipal principal = switch (role) {
            case Role.STUDENT -> new UserPrincipal(UUID.fromString("11111111-1111-1111-1111-111111111111"), role);
            case Role.TEACHER -> new UserPrincipal(UUID.fromString("33333333-3333-3333-3333-333333333333"), role);
            case Role.ADMIN -> new UserPrincipal(UUID.fromString("55555555-5555-5555-5555-555555555555"), role);
        };
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return true;
    }
}
