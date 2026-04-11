package ru.hse.sportclassbookingbackend.dto.auth;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank
        @Email
        String email,
        @NotBlank
        String password
) {

}
