package ru.hse.sportclassbookingbackend.dto.student;

import jakarta.validation.constraints.NotBlank;
import ru.hse.sportclassbookingbackend.model.HealthGroup;

import java.util.UUID;

public record StudentHealthGroupPatchRequest(

        @NotBlank
        Integer healthGroupId
) {
}
