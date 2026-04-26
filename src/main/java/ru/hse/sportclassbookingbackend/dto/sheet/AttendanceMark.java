package ru.hse.sportclassbookingbackend.dto.sheet;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttendanceMark(
        @Schema(description = "ID записи (sheet), на которую ставим отметку", example = "30000000-0000-0000-0000-000000000020")
        @NotNull
        UUID sheetId,
        @Schema(description = "true = пришёл, false = не пришёл", example = "true")
        @NotNull
        Boolean visited
) {
}
