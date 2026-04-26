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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.sportclassbookingbackend.dto.auth.AuthResponse;
import ru.hse.sportclassbookingbackend.dto.auth.LoginRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterStudentRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterTeacherRequest;
import ru.hse.sportclassbookingbackend.handler.ErrorResponse;
import ru.hse.sportclassbookingbackend.service.AuthService;

import java.util.UUID;

@Tag(name = "Auth", description = "Публичные эндпоинты: регистрация, логин, refresh, logout. Токен не требуется.")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Логин по email и паролю",
            description = "Возвращает пару access (JWT, ~30 мин) + refresh (UUID, хранится в Redis, ~30 дней)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токены выданы"),
            @ApiResponse(responseCode = "400", description = "Невалидный payload (пустое поле, не-email)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Email не найден или неверный пароль",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Регистрация студента",
            description = "Создаёт пользователя с ролью STUDENT и сразу логинит — возвращает access+refresh токены. Health group по умолчанию ставится в 5 ('Нет данных'); менять может ADMIN/TEACHER через PATCH /students/{id}/health-group."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь создан, токены выданы"),
            @ApiResponse(responseCode = "400", description = "Невалидный payload ИЛИ groupId/campusId не существуют",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email уже занят",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register/student")
    public ResponseEntity<AuthResponse> registerStudent(@RequestBody @Valid RegisterStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerStudent(request));
    }

    @Operation(
            summary = "Регистрация преподавателя",
            description = "Создаёт пользователя с ролью TEACHER и сразу логинит. Кампус фиксируется при регистрации."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь создан, токены выданы"),
            @ApiResponse(responseCode = "400", description = "Невалидный payload ИЛИ campusId не существует",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email уже занят",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register/teacher")
    public ResponseEntity<AuthResponse> registerTeacher(@RequestBody @Valid RegisterTeacherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerTeacher(request));
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
            @Parameter(description = "Refresh-токен (UUID), выданный при /login или /register", required = true)
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
