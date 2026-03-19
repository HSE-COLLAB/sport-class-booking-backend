package ru.hse.sportclassbookingbackend.dto.workouttype;

public record WorkoutTypePatchRequest(
        String title,
        Integer allowHealthGroupId
) {
}
