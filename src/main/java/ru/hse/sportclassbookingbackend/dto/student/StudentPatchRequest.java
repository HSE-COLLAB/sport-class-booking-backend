package ru.hse.sportclassbookingbackend.dto.student;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

@Schema(description = "ADMIN PATCH студента. Все поля опциональны — null игнорируется.")
public record StudentPatchRequest(
        @Email
        String email,

        @Pattern(regexp = "^(?!\\s*$).+", message = "password must not be blank")
        @Schema(description = "Новый пароль в plaintext — будет перехеширован BCrypt")
        String password,

        @Pattern(regexp = "^(?!\\s*$).+", message = "first name must not be blank")
        String firstName,

        @Pattern(regexp = "^(?!\\s*$).+", message = "last name must not be blank")
        String lastName,

        @Pattern(regexp = "^(?!\\s*$).+", message = "middle name must not be blank")
        String middleName,

        @Schema(description = "Флаг активности (admin может деактивировать/реактивировать)")
        Boolean isActive,

        @Schema(description = "Новая студенческая группа (UUID из GET /student-groups)")
        UUID groupId,

        @Schema(description = "Новая медгруппа (1..5)")
        Integer healthGroupId,

        @Schema(description = "Роль (устарело — смена роли через API не поддерживается и игнорируется)")
        String role
) {
}
