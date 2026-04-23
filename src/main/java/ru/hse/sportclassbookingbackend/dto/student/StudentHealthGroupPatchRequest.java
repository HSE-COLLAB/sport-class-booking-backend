package ru.hse.sportclassbookingbackend.dto.student;

import jakarta.validation.constraints.NotNull;

public record StudentHealthGroupPatchRequest(

        @NotNull
        Integer healthGroupId
) {
}
