package ru.hse.sportclassbookingbackend.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.sportclassbookingbackend.security.UserPrincipal;

// Контроллер для тестирования авторизации
@ConditionalOnProperty(name = "security.development.default-user.enabled", havingValue = "true")
@RestController
@RequestMapping("/test-auth")
public class TestController {

    @GetMapping("/common")
    public String commonEndpoint(@AuthenticationPrincipal UserPrincipal principal){
        return "Access for student, teacher and admin. " + principal.getId() + ", " + principal.getRole();
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public String onlyStudent(@AuthenticationPrincipal UserPrincipal principal){
        return "Access for only student. " + principal.getId() + ", " + principal.getRole();
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public String onlyTeacher(@AuthenticationPrincipal UserPrincipal principal){
        return "Access for only teacher. " + principal.getId() + ", " + principal.getRole();
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String onlyAdmin(@AuthenticationPrincipal UserPrincipal principal){
        return "Access for admin. " + principal.getId() + ", " + principal.getRole();
    }

    @GetMapping("/admin-teacher")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public String adminOrTeacher(@AuthenticationPrincipal UserPrincipal principal){
        return "Access for admin and teacher. " + principal.getId() + ", " + principal.getRole();
    }
}
