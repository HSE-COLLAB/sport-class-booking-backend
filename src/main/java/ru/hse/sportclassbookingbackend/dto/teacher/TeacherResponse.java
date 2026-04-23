package ru.hse.sportclassbookingbackend.dto.teacher;

import java.util.UUID;

public record TeacherResponse(
        UUID id,
        String email,
        String password,
        String firstName,
        String lastName,
        String middleName,
        String role,
        String position,
        Boolean isActive
) {
}
