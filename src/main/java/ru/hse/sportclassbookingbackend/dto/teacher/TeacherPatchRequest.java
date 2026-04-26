package ru.hse.sportclassbookingbackend.dto.teacher;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;


@Schema(description = "PATCH преподавателя: ADMIN любого, TEACHER только себя. Все поля опциональны.")
public record TeacherPatchRequest(
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
        String middleName,

        @Schema(description = "Роль (устарело — смена роли через API не поддерживается и игнорируется)")
        String role,

        @Schema(description = "Флаг активности (admin может деактивировать/реактивировать)")
        Boolean isActive,

        @Pattern(regexp = "^(?!\\s*$).+", message = "position must not be blank")
        @Schema(description = "Должность", example = "Старший преподаватель")
        String position
) {
}
