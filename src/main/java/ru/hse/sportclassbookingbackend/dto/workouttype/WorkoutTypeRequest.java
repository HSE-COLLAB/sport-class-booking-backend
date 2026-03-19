package ru.hse.sportclassbookingbackend.dto.workouttype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkoutTypeRequest(
        @NotBlank
        String title,
        @NotNull
        Integer allowHealthGroupId
) {
}
