package ru.hse.sportclassbookingbackend.dto.sheet;

import java.util.UUID;

public record AttendeeResponse(
        UUID sheetId,
        Boolean visited,
        StudentShortResponse student
) {
}
