package ru.hse.sportclassbookingbackend.dto.workouttype;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

@Schema(description = "PATCH типа тренировки. null-поля игнорируются.")
public record WorkoutTypePatchRequest(
        @Pattern(regexp = "^(?!\\s*$).+", message = "title must not be blank")
        String title,
        @Schema(description = "Полностью заменяет набор разрешённых медгрупп")
        Set<Integer> allowedHealthGroupIds
) {
}
