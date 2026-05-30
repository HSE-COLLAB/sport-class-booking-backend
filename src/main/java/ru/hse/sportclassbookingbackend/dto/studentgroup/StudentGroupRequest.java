package ru.hse.sportclassbookingbackend.dto.studentgroup;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record StudentGroupRequest(
        @NotBlank
        @Schema(example = "ФКН")
        String faculty,
        @NotBlank
        @Schema(example = "Программная инженерия")
        String academicMajor,
        @NotBlank
        @Schema(example = "ПИ-2024")
        String groupNumber
) {
}
