package ru.hse.sportclassbookingbackend.dto.teacher;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record TeacherResponse(
        @Schema(example = "33333333-3333-3333-3333-333333333333")
        UUID id,
        String email,
        String firstName,
        String lastName,
        String middleName,
        @Schema(description = "Роль", example = "TEACHER")
        String role,
        @Schema(description = "Должность", example = "Доцент")
        String position,
        Boolean isActive
) {
}
