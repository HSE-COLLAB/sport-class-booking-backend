package ru.hse.sportclassbookingbackend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterTeacherRequest(
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @NotBlank
        String middleName,
        @NotBlank
        @Email
        @Schema(example = "newteacher@mail.ru")
        String email,
        @NotBlank
        @Schema(description = "пароль в plaintext — будет захеширован BCrypt")
        String password,
        @NotBlank
        @Schema(description = "Должность", example = "Доцент")
        String position,
        @Schema(description = "ID кампуса (1=Москва, 2=СПб, 3=НН, 4=Пермь)", example = "3")
        @NotNull
        Integer campusId
) {
}
