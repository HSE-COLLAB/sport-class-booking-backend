package ru.hse.sportclassbookingbackend.dto.studentgroup;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "PATCH студенческой группы. null-поля игнорируются.")
public record StudentGroupPatchRequest(
        @Pattern(regexp = "^(?!\\s*$).+", message = "faculty must not be blank")
        String faculty,
        @Pattern(regexp = "^(?!\\s*$).+", message = "academicMajor must not be blank")
        String academicMajor,
        @Pattern(regexp = "^(?!\\s*$).+", message = "groupNumber must not be blank")
        String groupNumber
) {
}
