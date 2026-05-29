package ru.hse.sportclassbookingbackend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank
        @Email
        @Schema(example = "student@hse.ru")
        String email,
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "code must be 6 digits")
        @Schema(description = "6-значный PIN из письма", example = "473829")
        String code,
        @NotBlank
        @Schema(description = "Новый пароль (plaintext, будет захеширован BCrypt)", example = "N3wP@ssw0rd")
        String newPassword
) {
}
