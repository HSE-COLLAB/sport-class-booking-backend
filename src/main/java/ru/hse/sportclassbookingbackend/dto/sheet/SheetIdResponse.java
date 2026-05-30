package ru.hse.sportclassbookingbackend.dto.sheet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record SheetIdResponse(
        @Schema(description = "ID созданной записи (sheet)", example = "30000000-0000-0000-0000-000000000099")
        UUID id
) {
}
