package ru.hse.sportclassbookingbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.sportclassbookingbackend.dto.auth.AuthResponse;
import ru.hse.sportclassbookingbackend.dto.auth.ForgotPasswordRequest;
import ru.hse.sportclassbookingbackend.dto.auth.LoginRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterResponse;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterStudentRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterTeacherRequest;
import ru.hse.sportclassbookingbackend.dto.auth.ResendVerificationRequest;
import ru.hse.sportclassbookingbackend.dto.auth.ResetPasswordRequest;
import ru.hse.sportclassbookingbackend.handler.ErrorResponse;
import ru.hse.sportclassbookingbackend.service.AuthService;

import java.util.UUID;

@Tag(name = "Auth", description = "Публичные эндпоинты: регистрация, верификация email, логин, refresh, logout. Токен не требуется.")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Логин по email и паролю",
            description = "Возвращает пару access (JWT, ~30 мин) + refresh (UUID, хранится в Redis, ~30 дней). " +
                    "Если email пользователя не подтверждён — вернёт 401."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токены выданы"),
            @ApiResponse(responseCode = "400", description = "Невалидный payload (пустое поле, не-email)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Email не найден, неверный пароль, или email не подтверждён",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Регистрация студента",
            description = "Создаёт пользователя с ролью STUDENT и `email_verified=false`. " +
                    "Токены НЕ выдаются — на указанный email уходит письмо со ссылкой подтверждения. " +
                    "После клика по ссылке (см. /auth/verify-email) пользователь сможет залогиниться. " +
                    "Health group по умолчанию ставится в 5 ('Нет данных'); менять может ADMIN/TEACHER через PATCH /students/{id}/health-group."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Пользователь создан, письмо с подтверждением отправлено"),
            @ApiResponse(responseCode = "400", description = "Невалидный payload ИЛИ groupId/campusId не существуют",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email уже занят",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register/student")
    public ResponseEntity<RegisterResponse> registerStudent(@RequestBody @Valid RegisterStudentRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(authService.registerStudent(request));
    }

    @Operation(
            summary = "Регистрация преподавателя",
            description = "Создаёт пользователя с ролью TEACHER и `email_verified=false`. " +
                    "Токены НЕ выдаются — на указанный email уходит письмо со ссылкой подтверждения. " +
                    "Кампус фиксируется при регистрации."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Пользователь создан, письмо с подтверждением отправлено"),
            @ApiResponse(responseCode = "400", description = "Невалидный payload ИЛИ campusId не существует",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email уже занят",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register/teacher")
    public ResponseEntity<RegisterResponse> registerTeacher(@RequestBody @Valid RegisterTeacherRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(authService.registerTeacher(request));
    }

    @Operation(
            summary = "Подтверждение email по токену из письма",
            description = "Эндпоинт, на который ведёт ссылка из письма верификации (GET, чтобы работал клик из почтового клиента). " +
                    "Помечает пользователя как `email_verified=true`. Токен одноразовый, TTL 24 часа. " +
                    "Идемпотентен: если пользователь уже подтверждён, всё равно вернёт 204."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Email подтверждён"),
            @ApiResponse(responseCode = "400", description = "Токен невалидный, истёкший или уже использованный",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(
            @Parameter(description = "Verification-токен (UUID) из ссылки в письме", required = true)
            @RequestParam("token") UUID token) {
        authService.verifyEmail(token);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Повторная отправка письма верификации",
            description = "Если email зарегистрирован и не подтверждён — отправит новое письмо. " +
                    "Всегда возвращает 204 (даже если email неизвестен или уже подтверждён) — чтобы не раскрывать факт существования пользователя."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Запрос принят (письмо отправлено, если требовалось)"),
            @ApiResponse(responseCode = "400", description = "Невалидный payload (пустое поле, не-email)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody @Valid ResendVerificationRequest request) {
        authService.resendVerification(request.email());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Запрос сброса пароля по email",
            description = "Если email зарегистрирован — отправляет 6-значный PIN на почту, действующий 10 минут. " +
                    "Всегда возвращает 204 (даже если email неизвестен) — чтобы не раскрывать факт существования пользователя. " +
                    "Используется в паре с POST /auth/reset-password."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Запрос принят (письмо с PIN отправлено, если email известен)"),
            @ApiResponse(responseCode = "400", description = "Невалидный payload (пустое поле, не-email)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Сброс пароля по PIN из письма",
            description = "Принимает email, 6-значный PIN из письма и новый пароль. " +
                    "При успехе пароль обновляется и все активные refresh-токены этого юзера инвалидируются — " +
                    "придётся залогиниться заново через POST /auth/login. " +
                    "При 5+ неверных попытках в рамках одной сессии сброса — сессия гасится, нужно запросить новый PIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Пароль обновлён, активные сессии отозваны"),
            @ApiResponse(responseCode = "400", description = "Невалидный payload, либо PIN неверный/истёкший/слишком много попыток",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(request.email(), request.code(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Обновление access-токена",
            description = "Принимает refresh-токен в заголовке X-Refresh-Token. Возвращает новый access + тот же refresh (refresh не ротируется)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Новый access-токен выдан"),
            @ApiResponse(responseCode = "401", description = "Refresh-токен невалиден, истёк или отозван через /logout",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Parameter(description = "Refresh-токен (UUID), выданный при /login", required = true)
            @RequestHeader(value = "X-Refresh-Token") UUID refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @Operation(
            summary = "Logout — отзыв refresh-токена",
            description = "Удаляет refresh-токен из Redis. После этого /refresh вернёт 401. Access-токен (JWT) продолжает работать до истечения своего expiration — сервер не хранит access-токены и не может их отозвать."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Токен отозван (идемпотентно: повторный logout тем же токеном тоже вернёт 204)")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "Refresh-токен (UUID), который нужно отозвать", required = true)
            @RequestHeader(value = "X-Refresh-Token") UUID refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
