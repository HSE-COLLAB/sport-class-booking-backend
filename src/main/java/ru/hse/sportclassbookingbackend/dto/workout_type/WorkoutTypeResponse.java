package ru.hse.sportclassbookingbackend.dto.workout_type;

import java.util.UUID;

public record WorkoutTypeResponse(UUID id, String title, HealthGroupResponse allowHealthGroup) {
}
