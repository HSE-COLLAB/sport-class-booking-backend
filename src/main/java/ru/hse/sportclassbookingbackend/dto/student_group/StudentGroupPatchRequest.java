package ru.hse.sportclassbookingbackend.dto.student_group;

import jakarta.annotation.Nullable;

public record StudentGroupPatchRequest(@Nullable String faculty, @Nullable String academicMajor,
                                       @Nullable String groupNumber) {
}
