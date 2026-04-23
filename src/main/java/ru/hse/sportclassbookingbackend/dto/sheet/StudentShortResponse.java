package ru.hse.sportclassbookingbackend.dto.sheet;

import java.util.UUID;

public record StudentShortResponse(
        UUID id,
        String firstName,
        String lastName,
        String middleName,
        String group,
        String healthGroup
) {
}
