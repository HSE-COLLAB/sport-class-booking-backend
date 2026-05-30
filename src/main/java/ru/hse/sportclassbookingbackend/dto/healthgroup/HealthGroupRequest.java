package ru.hse.sportclassbookingbackend.dto.healthgroup;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record HealthGroupRequest(
        @NotBlank
        @Schema(description = "Описание медгруппы", example = "Основная группа")
        String description
) {
}
