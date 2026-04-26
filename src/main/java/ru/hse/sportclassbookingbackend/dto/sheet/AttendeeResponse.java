package ru.hse.sportclassbookingbackend.dto.sheet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record AttendeeResponse(
        @Schema(description = "ID записи студента на урок", example = "30000000-0000-0000-0000-000000000020")
        UUID sheetId,
        @Schema(description = "Отметка посещения. null — ещё не отмечено.")
        Boolean visited,
        StudentShortResponse student
) {
}
