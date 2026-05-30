package ru.hse.sportclassbookingbackend.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.hse.sportclassbookingbackend.security.UserPrincipal;

import java.io.IOException;
import java.util.UUID;

public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);

    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_ROLE = "role";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        MDC.put(MDC_REQUEST_ID, UUID.randomUUID().toString());

        try {
            filterChain.doFilter(request, response);
        } finally {
            populateUserMdcFromSecurityContext();
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
            int status = response.getStatus();
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String query = request.getQueryString();
            String fullUri = query == null ? uri : uri + "?" + query;

            if (status >= 500) {
                log.error("{} {} -> {} ({} ms)", method, fullUri, status, durationMs);
            } else if (status >= 400) {
                log.warn("{} {} -> {} ({} ms)", method, fullUri, status, durationMs);
            } else {
                log.info("{} {} -> {} ({} ms)", method, fullUri, status, durationMs);
            }

            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_ROLE);
        }
    }

    private void populateUserMdcFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal up) {
            MDC.put(MDC_USER_ID, String.valueOf(up.getId()));
            if (up.getRole() != null) {
                MDC.put(MDC_ROLE, up.getRole().name());
            }
        }
    }
}
