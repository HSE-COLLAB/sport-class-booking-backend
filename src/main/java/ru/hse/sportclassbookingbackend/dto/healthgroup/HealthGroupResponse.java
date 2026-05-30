package ru.hse.sportclassbookingbackend.dto.healthgroup;

import io.swagger.v3.oas.annotations.media.Schema;

public record HealthGroupResponse(
        @Schema(description = "Id медгруппы (1..5)", example = "1")
        Integer id,
        @Schema(example = "Основная группа")
        String description
) {
}
