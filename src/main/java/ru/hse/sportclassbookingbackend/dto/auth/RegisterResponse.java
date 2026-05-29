package ru.hse.sportclassbookingbackend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ на регистрацию. Токены НЕ выдаются — сначала нужно подтвердить email по ссылке из письма.")
public record RegisterResponse(
        @Schema(description = "Информационное сообщение для пользователя",
                example = "Verification email sent. Please check your inbox to activate your account.")
        String message,
        @Schema(description = "Email, на который ушло письмо. Используется для повторной отправки через /auth/resend-verification.",
                example = "newstudent@mail.ru")
        String email
) {
    public static RegisterResponse of(String email) {
        return new RegisterResponse(
                "Verification email sent. Please check your inbox to activate your account.",
                email
        );
    }
}
