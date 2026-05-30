package ru.hse.sportclassbookingbackend.dto.auth;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "email пользователя", example = "student1@mail.ru")
        @NotBlank
        @Email
        String email,

        @Schema(description = "пароль в plaintext — сервер сверит с BCrypt-хешем", example = "test")
        @NotBlank
        String password
) {

}
