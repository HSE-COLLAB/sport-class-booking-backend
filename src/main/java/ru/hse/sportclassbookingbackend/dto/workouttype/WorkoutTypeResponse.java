package ru.hse.sportclassbookingbackend.dto.workouttype;

import java.util.UUID;

public record WorkoutTypeResponse(
        UUID id,
        String title,
        HealthGroupResponse allowHealthGroup
) {
}
