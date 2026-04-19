package ru.hse.sportclassbookingbackend.dto.workouttype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record WorkoutTypeRequest(
        @NotBlank
        String title,
        @NotEmpty
        Set<Integer> allowedHealthGroupIds
) {
}
