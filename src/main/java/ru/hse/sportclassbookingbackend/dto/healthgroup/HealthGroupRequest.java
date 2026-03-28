package ru.hse.sportclassbookingbackend.dto.healthgroup;

import jakarta.validation.constraints.NotBlank;

public record HealthGroupRequest(
        @NotBlank
        String description
) {
}
