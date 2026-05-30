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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupRequest;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupResponse;
import ru.hse.sportclassbookingbackend.handler.ErrorResponse;
import ru.hse.sportclassbookingbackend.service.HealthGroupService;

import java.util.List;

@Tag(name = "HealthGroups", description = "Медицинские группы (1..5). Фиксированный справочник — только GET + PUT description.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/health-groups")
@RequiredArgsConstructor
public class HealthGroupController {

    private final HealthGroupService healthGroupService;

    @Operation(summary = "Список медгрупп")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Отсутствует авторизация",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<HealthGroupResponse>> getAll() {
        return ResponseEntity.ok(healthGroupService.getAll());
    }

    @Operation(summary = "Получить медгруппу по id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Медгруппа"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Отсутствует авторизация",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Не найдена (допустимые id: 1..5)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<HealthGroupResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(healthGroupService.getById(id));
    }

    @Operation(summary = "Обновить описание медгруппы", description = "Можно менять только description. Id фиксированы (1..5).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обновлено"),
            @ApiResponse(responseCode = "400", description = "Пустой description",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Не ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Медгруппа с таким id не существует",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HealthGroupResponse> update(
            @PathVariable int id,
            @RequestBody @Valid HealthGroupRequest request
    ) {
        return ResponseEntity.ok(healthGroupService.update(id, request));
    }
}
