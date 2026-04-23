package ru.hse.sportclassbookingbackend.dto.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record StudentSelfUpdateRequest(
        @Email
        String email,

        @Pattern(regexp = "^(?!\\s*$).+", message = "password must not be blank")
        String password,

        @Pattern(regexp = "^(?!\\s*$).+", message = "first name must not be blank")
        String firstName,

        @Pattern(regexp = "^(?!\\s*$).+", message = "last name must not be blank")
        String lastName,

        @Pattern(regexp = "^(?!\\s*$).+", message = "middle name must not be blank")
        String middleName
) {
}
