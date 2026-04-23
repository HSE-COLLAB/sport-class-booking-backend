package ru.hse.sportclassbookingbackend.dto.teacher;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;


public record TeacherPatchRequest(
        @Email
        String email,

        @Pattern(regexp = "^(?!\\s*$).+", message = "password must not be blank")
        String password,

        @Pattern(regexp = "^(?!\\s*$).+", message = "first name must not be blank")
        String firstName,

        @Pattern(regexp = "^(?!\\s*$).+", message = "last name must not be blank")
        String lastName,

        @Pattern(regexp = "^(?!\\s*$).+", message = "middle name must not be blank")
        String middleName,

        @Pattern(regexp = "^(?!\\s*$).+", message = "role must not be blank")
        String role,

        Boolean isActive,

        @Pattern(regexp = "^(?!\\s*$).+", message = "position must not be blank")
        String position
) {
}
