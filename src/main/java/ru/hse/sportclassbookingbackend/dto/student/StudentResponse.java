package ru.hse.sportclassbookingbackend.dto.student;

import java.util.UUID;

public record StudentResponse(
        UUID id,
        UUID groupId,
        Integer healthGroupId
) {
}
