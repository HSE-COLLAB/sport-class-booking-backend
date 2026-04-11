package ru.hse.sportclassbookingbackend.dto.workouttype;

import jakarta.validation.constraints.Pattern;

public record WorkoutTypePatchRequest(
        @Pattern(regexp = "^(?!\\s*$).+", message = "title must not be blank")
        String title,
        Integer allowHealthGroupId
) {
}
