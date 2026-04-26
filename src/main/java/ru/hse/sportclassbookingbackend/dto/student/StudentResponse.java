package ru.hse.sportclassbookingbackend.dto.student;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record StudentResponse(
        @Schema(example = "55555555-5555-5555-5555-555555555555")
        UUID id,
        @Schema(description = "ID студенческой группы (GET /student-groups)",
                example = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        UUID groupId,
        @Schema(description = "ID медгруппы (1..5)", example = "1")
        Integer healthGroupId,
        String email,
        String firstName,
        String lastName,
        String middleName,
        @Schema(description = "Роль пользователя", example = "STUDENT")
        String role,
        Boolean isActive
) {
}
