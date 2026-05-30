package ru.hse.sportclassbookingbackend.dto.studentgroup;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record StudentGroupResponse(
        @Schema(example = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        UUID id,
        String faculty,
        String academicMajor,
        @Schema(example = "ПИ-2024")
        String groupNumber
) {
}
