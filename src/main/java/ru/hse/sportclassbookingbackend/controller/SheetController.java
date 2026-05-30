package ru.hse.sportclassbookingbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.sportclassbookingbackend.dto.sheet.AttendeeResponse;
import ru.hse.sportclassbookingbackend.dto.sheet.BulkAttendanceRequest;
import ru.hse.sportclassbookingbackend.dto.sheet.SheetIdResponse;
import ru.hse.sportclassbookingbackend.handler.ErrorResponse;
import ru.hse.sportclassbookingbackend.security.UserPrincipal;
import ru.hse.sportclassbookingbackend.service.SheetService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Sheets", description = "Записи студентов на занятия и отметка посещаемости.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/lessons/{lessonId}/sheets")
@RequiredArgsConstructor
public class SheetController {

    private final SheetService sheetService;

    @Operation(
            summary = "Записаться на занятие (студент)",
            description = "Студент записывается на урок своего кампуса. Проверки: урок ACTIVE, ещё не начался, кампус совпадает, медгруппа студента входит в allowedHealthGroups типа тренировки, нет дубля, есть свободные места, нет пересечения по времени с другими ACTIVE-записями студента."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Запись создана"),
            @ApiResponse(responseCode = "400", description = "Урок уже начался / health group студента не входит в allowedHealthGroups типа тренировки",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Нет роли STUDENT, либо студент пытается записаться на урок в чужом кампусе",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Урок не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Урок CANCELLED / уже записан / мест нет / пересечение по времени с другим занятием",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SheetIdResponse> register(
            @Parameter(description = "ID урока", required = true)
            @PathVariable UUID lessonId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sheetService.register(lessonId, principal));
    }

    @Operation(
            summary = "Отменить запись на занятие",
            description = "STUDENT может снять свою запись. TEACHER — любую на своём уроке. ADMIN — любую. Только до начала урока."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Запись удалена"),
            @ApiResponse(responseCode = "400", description = "Sheet не принадлежит указанному lessonId / урок уже начался",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Нет роли STUDENT/TEACHER/ADMIN, либо STUDENT снимает чужую запись, либо TEACHER снимает запись на чужом уроке",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sheet не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Урок CANCELLED — отмена записи невозможна",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{sheetId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID lessonId,
            @Parameter(description = "ID записи (sheet) из ответа POST /lessons/{lessonId}/sheets")
            @PathVariable UUID sheetId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        sheetService.cancel(lessonId, sheetId, principal);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Список записавшихся на занятие",
            description = "Доступно любому авторизованному. Отсортировано по lastName ASC."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список записей"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token / отсутствует авторизация",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Урок не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<AttendeeResponse>> getAttendees(@PathVariable UUID lessonId) {
        return ResponseEntity.ok(sheetService.getAttendees(lessonId));
    }

    @Operation(
            summary = "Отметить посещаемость (массово)",
            description = "TEACHER отмечает только на своём уроке. ADMIN — на любом. Только после начала урока. Запрос принимает массив { sheetId, visited }. Все sheetId должны принадлежать этому уроку."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Посещаемость обновлена"),
            @ApiResponse(responseCode = "400", description = "Урок ещё не начался / sheetId не принадлежит этому уроку / пустой marks",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Нет роли TEACHER/ADMIN, либо TEACHER отмечает на чужом уроке",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Урок не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Урок CANCELLED — отметка невозможна",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<AttendeeResponse>> markAttendance(
            @PathVariable UUID lessonId,
            @RequestBody @Valid BulkAttendanceRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(sheetService.markAttendance(lessonId, request, principal));
    }
}
