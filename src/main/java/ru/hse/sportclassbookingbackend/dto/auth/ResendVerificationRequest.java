package ru.hse.sportclassbookingbackend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(
        @NotBlank
        @Email
        @Schema(example = "newstudent@mail.ru")
        String email
) {
}
