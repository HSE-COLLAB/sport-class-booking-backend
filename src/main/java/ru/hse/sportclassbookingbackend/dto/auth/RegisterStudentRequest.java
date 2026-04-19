package ru.hse.sportclassbookingbackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterStudentRequest(
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
        @NotNull
        UUID groupId,
        @NotNull
        Integer campusId
) {

}
