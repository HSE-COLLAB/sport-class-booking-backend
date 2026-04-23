package ru.hse.sportclassbookingbackend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.aspectj.weaver.ast.Not;

public record UserRequest(
        @NotBlank
        String email,

        @NotBlank
        String password,

        @NotBlank
        String first_name,

        @NotBlank
        String last_name,

        @NotBlank
        String middle_name,

        @NotBlank
        String role,

        @NotNull
        Boolean is_active
) {
}