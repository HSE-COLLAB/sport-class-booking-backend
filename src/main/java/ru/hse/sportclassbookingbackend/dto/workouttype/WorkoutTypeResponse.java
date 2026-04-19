package ru.hse.sportclassbookingbackend.dto.workouttype;

import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupResponse;

import java.util.List;
import java.util.UUID;

public record WorkoutTypeResponse(
        UUID id,
        String title,
        List<HealthGroupResponse> allowedHealthGroups
) {
}
