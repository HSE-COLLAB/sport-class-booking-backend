package ru.hse.sportclassbookingbackend.dto.student;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Студент обновляет собственный профиль. group/healthGroup менять нельзя.")
public record StudentSelfUpdateRequest(
        @Email
        String email,

        @Pattern(regexp = "^(?!\\s*$).+", message = "password must not be blank")
        @Schema(description = "Новый пароль — будет перехеширован BCrypt")
        String password,

        @Pattern(regexp = "^(?!\\s*$).+", message = "first name must not be blank")
        String firstName,

        @Pattern(regexp = "^(?!\\s*$).+", message = "last name must not be blank")
        String lastName,

        @Pattern(regexp = "^(?!\\s*$).+", message = "middle name must not be blank")
        String middleName
) {
}
