package ru.hse.sportclassbookingbackend.dto.student;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import ru.hse.sportclassbookingbackend.model.HealthGroup;

import java.util.UUID;

public record StudentHealthGroupPatchRequest(
        @NotBlank
        @Schema(description = "Новая медгруппа (1..5)", example = "2")
        Integer healthGroupId
) {
}
