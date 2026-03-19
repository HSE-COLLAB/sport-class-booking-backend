package ru.hse.sportclassbookingbackend.dto.studentgroup;

import jakarta.validation.constraints.NotBlank;

public record StudentGroupRequest(
        @NotBlank
        String faculty,
        @NotBlank
        String academicMajor,
        @NotBlank
        String groupNumber
) {
}
