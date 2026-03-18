package ru.hse.sportclassbookingbackend.dto.student_group;

import java.util.UUID;

public record StudentGroupResponse(UUID id, String faculty, String academicMajor, String groupNumber) {
}
