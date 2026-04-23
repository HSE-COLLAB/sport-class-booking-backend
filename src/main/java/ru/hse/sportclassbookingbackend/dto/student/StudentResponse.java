package ru.hse.sportclassbookingbackend.dto.student;

import java.util.UUID;

public record StudentResponse(
        UUID id,
        UUID groupId,
        Integer healthGroupId,
        String email,
        String firstName,
        String lastName,
        String middleName,
        String role,
        Boolean isActive

) {
}
