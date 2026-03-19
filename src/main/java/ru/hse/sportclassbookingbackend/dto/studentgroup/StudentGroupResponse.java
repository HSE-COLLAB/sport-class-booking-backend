package ru.hse.sportclassbookingbackend.dto.studentgroup;

import java.util.UUID;

public record StudentGroupResponse(
        UUID id,
        String faculty,
        String academicMajor,
        String groupNumber
) {
}
