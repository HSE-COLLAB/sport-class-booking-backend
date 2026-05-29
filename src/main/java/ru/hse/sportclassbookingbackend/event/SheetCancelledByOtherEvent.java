package ru.hse.sportclassbookingbackend.event;

import java.util.UUID;

public record SheetCancelledByOtherEvent(
        UUID sheetId,
        UUID lessonId,
        UUID studentId,
        UUID cancelledById
) {
}
