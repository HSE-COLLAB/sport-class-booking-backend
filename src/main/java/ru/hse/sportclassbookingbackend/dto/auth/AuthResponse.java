package ru.hse.sportclassbookingbackend.dto.auth;


import java.util.UUID;

public record AuthResponse(
        String accessToken,
        UUID refreshToken
) {
    public static AuthResponse of(String accessToken, UUID refreshToken) {
        return new AuthResponse(accessToken, refreshToken);
    }
}
