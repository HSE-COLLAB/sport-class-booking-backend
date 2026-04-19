package ru.hse.sportclassbookingbackend.dto.workouttype;

import jakarta.validation.constraints.Pattern;

import java.util.Set;

public record WorkoutTypePatchRequest(
        @Pattern(regexp = "^(?!\\s*$).+", message = "title must not be blank")
        String title,
        Set<Integer> allowedHealthGroupIds
) {
}
