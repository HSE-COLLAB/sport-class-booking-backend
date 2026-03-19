package ru.hse.sportclassbookingbackend.dto.studentgroup;

public record StudentGroupPatchRequest(
        String faculty,
        String academicMajor,
        String groupNumber
) {
}
