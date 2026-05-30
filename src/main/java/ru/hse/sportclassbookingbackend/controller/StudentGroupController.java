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
import ru.hse.sportclassbookingbackend.dto.studentgroup.StudentGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.studentgroup.StudentGroupRequest;
import ru.hse.sportclassbookingbackend.dto.studentgroup.StudentGroupResponse;
import ru.hse.sportclassbookingbackend.handler.ErrorResponse;
import ru.hse.sportclassbookingbackend.service.StudentGroupService;

import java.util.List;
import java.util.UUID;

@Tag(name = "StudentGroups", description = "Студенческие группы (факультет/программа/номер). CRUD — только для ADMIN.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/student-groups")
@RequiredArgsConstructor
public class StudentGroupController {

    private final StudentGroupService studentGroupService;

    @Operation(summary = "Список студенческих групп")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Отсутствует авторизация",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<StudentGroupResponse>> getAll() {
        return ResponseEntity.ok(studentGroupService.getAll());
    }

    // TODO: GET /{id} — вернуть группу со списком студентов
    // Реализовать когда будет готов студентский функционал

    @Operation(summary = "Создать студенческую группу")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Создана"),
            @ApiResponse(responseCode = "400", description = "Невалидный payload (пустые поля)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentGroupResponse> create(@RequestBody @Valid StudentGroupRequest studentGroupRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentGroupService.create(studentGroupRequest));
    }

    @Operation(summary = "Обновить студенческую группу", description = "Частичное обновление: null-поля игнорируются.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обновлена"),
            @ApiResponse(responseCode = "400", description = "Пустые значения переданы",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Группа не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentGroupResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid StudentGroupPatchRequest request
    ) {
        return ResponseEntity.ok(studentGroupService.update(id, request));
    }

    @Operation(
            summary = "Удалить студенческую группу",
            description = "Хард-удаление. У студентов, привязанных к этой группе, group_id занулится (ON DELETE SET NULL)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Удалена (idempotent)"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        studentGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
