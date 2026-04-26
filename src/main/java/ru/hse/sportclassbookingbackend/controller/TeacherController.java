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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.sportclassbookingbackend.dto.teacher.TeacherPatchRequest;
import ru.hse.sportclassbookingbackend.dto.teacher.TeacherResponse;
import ru.hse.sportclassbookingbackend.handler.ErrorResponse;
import ru.hse.sportclassbookingbackend.service.TeacherService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Teachers", description = "Управление преподавателями: листинг (admin/teacher), self-update, смена должности.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
public class TeacherController {
    private final TeacherService teacherService;

    @Operation(
            summary = "Деактивировать преподавателя",
            description = "Ставит isActive=false. ADMIN — любого. Преподаватель — только себя. Silent no-op если не найден."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Выполнено"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN и не сам себя",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Обновить данные преподавателя (admin или self)",
            description = "ADMIN — любого, TEACHER — только себя. Пароль перехешируется BCrypt."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обновлено"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN и не сам себя",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Преподаватель не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<TeacherResponse> update(@PathVariable UUID id, @RequestBody @Valid TeacherPatchRequest request) {
        return ResponseEntity.ok(teacherService.update(id, request));
    }

    @Operation(summary = "Получить преподавателя по id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Преподаватель"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN и не сам себя",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Преподаватель не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<TeacherResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(teacherService.getById(id));
    }

    @Operation(summary = "Список активных преподавателей")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN/TEACHER",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<TeacherResponse>> getAll() {
        return ResponseEntity.ok(teacherService.getAll());
    }
}
