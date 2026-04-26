package ru.hse.sportclassbookingbackend.dto.auth;


import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Пара токенов, выдаваемая при логине/регистрации/refresh.")
public record AuthResponse(
        @Schema(description = "JWT access-токен. Прикладывать в Authorization: Bearer <...>. Живёт ~30 минут.")
        String accessToken,
        @Schema(description = "UUID refresh-токена. Использовать в X-Refresh-Token для /auth/refresh и /auth/logout.",
                example = "550e8400-e29b-41d4-a716-446655440000")
        UUID refreshToken
) {
    public static AuthResponse of(String accessToken, UUID refreshToken) {
        return new AuthResponse(accessToken, refreshToken);
    }
}
