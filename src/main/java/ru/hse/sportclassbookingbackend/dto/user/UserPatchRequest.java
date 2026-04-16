package ru.hse.sportclassbookingbackend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record UserPatchRequest(
        @Email
        String email,

        @Pattern(regexp = "^(?!\\s*$).+", message = "password must not be blank")
        String password,

        @Pattern(regexp = "^(?!\\s*$).+", message = "first must not be blank")
        String first_name,

        @Pattern(regexp = "^(?!\\s*$).+", message = "last name must not be blank")
        String last_name,

        @Pattern(regexp = "^(?!\\s*$).+", message = "middle name must not be blank")
        String middle_name,

        @Pattern(regexp = "^(?!\\s*$).+", message = "role must not be blank")
        String role,

        Boolean is_active
) {
}
