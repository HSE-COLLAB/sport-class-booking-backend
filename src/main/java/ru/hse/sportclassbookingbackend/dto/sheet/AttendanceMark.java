package ru.hse.sportclassbookingbackend.dto.sheet;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttendanceMark(
        @NotNull
        UUID sheetId,
        @NotNull
        Boolean visited
) {
}
