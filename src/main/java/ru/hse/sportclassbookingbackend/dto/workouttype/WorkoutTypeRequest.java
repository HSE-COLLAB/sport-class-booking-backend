package ru.hse.sportclassbookingbackend.dto.workouttype;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record WorkoutTypeRequest(
        @NotBlank
        @Schema(example = "Аэробика")
        String title,
        @NotEmpty
        @Schema(description = "Набор id медгрупп, для которых разрешён этот тип тренировки (из /health-groups)",
                example = "[1,2]")
        Set<Integer> allowedHealthGroupIds
) {
}
