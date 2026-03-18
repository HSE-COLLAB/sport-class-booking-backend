package ru.hse.sportclassbookingbackend.dto.student_group;

import jakarta.validation.constraints.NotBlank;

public record StudentGroupRequest(@NotBlank String faculty, @NotBlank String academicMajor,
                                  @NotBlank String groupNumber) {

}