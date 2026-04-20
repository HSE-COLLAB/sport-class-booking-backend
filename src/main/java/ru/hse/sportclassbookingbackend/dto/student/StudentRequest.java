package ru.hse.sportclassbookingbackend.dto.student;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record StudentRequest(
        @NotBlank
        UUID groupId,

        @NotBlank
        Integer healthGroupId
) {
}
