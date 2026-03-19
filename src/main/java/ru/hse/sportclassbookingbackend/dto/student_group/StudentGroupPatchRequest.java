package ru.hse.sportclassbookingbackend.dto.student_group;

import jakarta.annotation.Nullable;

public record StudentGroupPatchRequest( String faculty,  String academicMajor,
                                        String groupNumber) {
}
