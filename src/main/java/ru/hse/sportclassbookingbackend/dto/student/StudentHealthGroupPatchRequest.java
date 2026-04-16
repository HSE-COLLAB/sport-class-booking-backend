package ru.hse.sportclassbookingbackend.dto.student;

import jakarta.validation.constraints.NotBlank;
import ru.hse.sportclassbookingbackend.model.HealthGroup;

public record StudentHealthGroupPatchRequest(

        @NotBlank
        HealthGroup health_group_id
) {
}
