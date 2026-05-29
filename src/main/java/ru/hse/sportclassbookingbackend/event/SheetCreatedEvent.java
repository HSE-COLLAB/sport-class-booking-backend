package ru.hse.sportclassbookingbackend.event;

import java.util.UUID;

public record SheetCreatedEvent(
        UUID sheetId,
        UUID lessonId,
        UUID studentId
) {
}
