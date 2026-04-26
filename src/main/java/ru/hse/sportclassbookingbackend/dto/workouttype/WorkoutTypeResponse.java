package ru.hse.sportclassbookingbackend.dto.workouttype;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupResponse;

import java.util.List;
import java.util.UUID;

public record WorkoutTypeResponse(
        @Schema(example = "10000000-0000-0000-0000-000000000001")
        UUID id,
        @Schema(example = "Волейбол")
        String title,
        @Schema(description = "Медгруппы, для которых разрешён этот тип тренировки")
        List<HealthGroupResponse> allowedHealthGroups
) {
}
