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
import ru.hse.sportclassbookingbackend.dto.student.StudentHealthGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.student.StudentResponse;
import ru.hse.sportclassbookingbackend.dto.student.StudentPatchRequest;
import ru.hse.sportclassbookingbackend.dto.student.StudentSelfUpdateRequest;
import ru.hse.sportclassbookingbackend.handler.ErrorResponse;
import ru.hse.sportclassbookingbackend.service.StudentService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Students", description = "Управление студентами: листинг (admin/teacher), смена health-group, self-update.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService studentService;

    @Operation(
            summary = "Деактивировать студента",
            description = "Ставит isActive=false. ADMIN может деактивировать любого студента. Студент — только себя. Silent no-op, если такого студента нет."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Выполнено (в т.ч. если студент не найден или уже неактивен)"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN и не сам себя",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Обновить данные студента (admin)",
            description = "Админский PATCH: email, пароль, ФИО, isActive, group, health group. Пароль перехешируется BCrypt."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обновлено"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Студент не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentResponse> update(@PathVariable UUID id, @RequestBody @Valid StudentPatchRequest request) {
        return ResponseEntity.ok(studentService.update(id, request));
    }

    @Operation(
            summary = "Сменить медгруппу студента",
            description = "Отдельный узкий эндпоинт — преподаватели тоже могут менять медгруппу, в отличие от общего PATCH."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Медгруппа изменена"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN/TEACHER",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Студент не найден ИЛИ healthGroupId не существует",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/health-group")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<StudentResponse> updateHealthGroup(@PathVariable UUID id, @RequestBody @Valid StudentHealthGroupPatchRequest request) {
        return ResponseEntity.ok(studentService.updateHealthGroup(id, request));
    }

    @Operation(summary = "Получить студента по id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Студент"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN/TEACHER и не сам себя",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Студент не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER') or #id == authentication.principal.id")
    public ResponseEntity<StudentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.getById(id));
    }

    @Operation(summary = "Список активных студентов", description = "Только isActive=true.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN/TEACHER",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<StudentResponse>> getAll() {
        return ResponseEntity.ok(studentService.getAll());
    }

    @Operation(
            summary = "Self-update студента",
            description = "Студент обновляет собственные email/пароль/ФИО. Менять group/healthGroup через self-update нельзя — они идут через админские эндпоинты."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обновлено"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "id в пути не совпадает с authenticated user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Студент не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/self-update")
    @PreAuthorize("#id == authentication.principal.id")
    public ResponseEntity<StudentResponse> selfUpdate(@PathVariable UUID id, @RequestBody @Valid StudentSelfUpdateRequest request) {
        return ResponseEntity.ok(studentService.selfUpdate(id, request));
    }
}
