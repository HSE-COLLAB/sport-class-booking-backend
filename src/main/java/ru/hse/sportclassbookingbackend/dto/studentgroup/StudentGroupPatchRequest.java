package ru.hse.sportclassbookingbackend.dto.studentgroup;

import jakarta.validation.constraints.Pattern;

public record StudentGroupPatchRequest(
        @Pattern(regexp = "^(?!\\s*$).+", message = "faculty must not be blank")
        String faculty,
        @Pattern(regexp = "^(?!\\s*$).+", message = "academicMajor must not be blank")
        String academicMajor,
        @Pattern(regexp = "^(?!\\s*$).+", message = "groupNumber must not be blank")
        String groupNumber
) {
}
