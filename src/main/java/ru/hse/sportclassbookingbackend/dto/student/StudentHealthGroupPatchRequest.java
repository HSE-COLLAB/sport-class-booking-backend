package ru.hse.sportclassbookingbackend.dto.student;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record StudentHealthGroupPatchRequest(
        @NotNull
        @Schema(description = "Новая медгруппа (1..5)", example = "2")
        Integer healthGroupId
) {
}
