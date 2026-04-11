package ru.hse.sportclassbookingbackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterTeacherRequest(
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @NotBlank
        String middleName,
        @NotBlank
        @Email
        String email,
        @NotBlank
        String password,
        @NotBlank
        String position
) {
}
