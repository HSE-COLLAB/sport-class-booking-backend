package ru.hse.sportclassbookingbackend.dto.auth;


import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank
        String email,
        @NotBlank
        String password
) {

}
