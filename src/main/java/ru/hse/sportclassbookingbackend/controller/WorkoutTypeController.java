package ru.hse.sportclassbookingbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypePatchRequest;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeRequest;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeResponse;
import ru.hse.sportclassbookingbackend.handler.ErrorResponse;
import ru.hse.sportclassbookingbackend.service.WorkoutTypeService;

import java.util.List;
import java.util.UUID;

@Tag(name = "WorkoutTypes", description = "Типы тренировок: CRUD. Каждый тип ссылается на набор разрешённых медгрупп.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/workout-types")
@RequiredArgsConstructor
public class WorkoutTypeController {

    private final WorkoutTypeService workoutTypeService;

    @Operation(summary = "Список активных типов тренировок", description = "Только isActive=true.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Отсутствует авторизация",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<WorkoutTypeResponse>> getAll() {
        return ResponseEntity.ok(workoutTypeService.getAll());
    }

    @Operation(summary = "Получить активный тип тренировки по id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Тип тренировки"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Отсутствует авторизация",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Не найден или неактивен",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<WorkoutTypeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(workoutTypeService.getById(id));
    }

    @Operation(summary = "Создать тип тренировки", description = "ADMIN/TEACHER. allowedHealthGroupIds — непустой набор id из /health-groups.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Создан"),
            @ApiResponse(responseCode = "400", description = "Невалидный payload (пустой title или allowedHealthGroupIds) / id медгруппы не существуют",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN/TEACHER",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<WorkoutTypeResponse> create(@RequestBody @Valid WorkoutTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutTypeService.create(request));
    }

    @Operation(summary = "Обновить тип тренировки", description = "Менять можно title и/или allowedHealthGroupIds. Нельзя редактировать неактивный.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обновлено"),
            @ApiResponse(responseCode = "400", description = "Невалидный payload / id медгрупп не существуют",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN/TEACHER",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Не найден или неактивен",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<WorkoutTypeResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid WorkoutTypePatchRequest request
    ) {
        return ResponseEntity.ok(workoutTypeService.update(id, request));
    }

    @Operation(summary = "Деактивировать тип тренировки", description = "Soft delete — ставит isActive=false. Silent no-op если уже неактивен.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Выполнено"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN/TEACHER",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        workoutTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
