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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonPatchRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonResponse;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonTimeStatus;
import ru.hse.sportclassbookingbackend.dto.lesson.RecurringLessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.RecurringLessonResponse;
import ru.hse.sportclassbookingbackend.dto.sheet.MyLessonResponse;
import ru.hse.sportclassbookingbackend.handler.ErrorResponse;
import ru.hse.sportclassbookingbackend.security.UserPrincipal;
import ru.hse.sportclassbookingbackend.service.LessonService;
import ru.hse.sportclassbookingbackend.service.SheetService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "Lessons", description = "Занятия: поиск, создание (teacher/admin), редактирование, отмена, регулярные.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final SheetService sheetService;

    @Operation(
            summary = "Список занятий с фильтрами",
            description = "Поиск по кампусу, типу тренировки, преподавателю, времени, месту, медгруппе студента. По умолчанию: ACTIVE + CANCELLED, все временные статусы. Отсортировано по startTime ASC (кроме случая, когда запрошено только PAST — тогда DESC)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Постраничный список"),
            @ApiResponse(responseCode = "400", description = "Невалидные параметры (campusId не существует, to <= from, myHealthGroup=true но не-студент)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Отсутствует авторизация",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<LessonResponse>> getAll(
            @Parameter(description = "ID кампуса (обязательно). Список: GET /campuses отсутствует — см. seed.", required = true, example = "3")
            @RequestParam Integer campusId,
            @Parameter(description = "Список ID типов тренировок (UUID). Фильтр ИЛИ — любое совпадение.")
            @RequestParam(required = false) List<UUID> workoutTypeId,
            @Parameter(description = "ID преподавателя (UUID). Фильтрует занятия конкретного преподавателя.")
            @RequestParam(required = false) UUID teacherId,
            @Parameter(description = "Нижняя граница интервала (локальное время кампуса, ISO без TZ).", example = "2026-05-05T00:00:00")
            @RequestParam(required = false) LocalDateTime from,
            @Parameter(description = "Верхняя граница интервала (локальное время кампуса, ISO без TZ).", example = "2026-05-07T23:59:59")
            @RequestParam(required = false) LocalDateTime to,
            @Parameter(description = "Подстрока в названии места (case-insensitive).")
            @RequestParam(required = false) String place,
            @Parameter(description = "Только для STUDENT: показывать только занятия, доступные по медгруппе студента.")
            @RequestParam(required = false) Boolean myHealthGroup,
            @Parameter(description = "Статусы по времени относительно now.")
            @RequestParam(required = false) List<LessonTimeStatus> status,
            @Parameter(description = "Если false, исключить из выдачи уроки со статусом CANCELLED. По умолчанию true (включены).")
            @RequestParam(required = false) Boolean includeCancelled,
            @Parameter(description = "Номер страницы с 0.")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы.")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                lessonService.getAll(campusId, workoutTypeId, teacherId, from, to, place, myHealthGroup, status,
                        includeCancelled, PageRequest.of(page, size), principal)
        );
    }

    @Operation(
            summary = "Мои занятия (для студента)",
            description = "Постраничный список занятий, на которые записан текущий студент или которые ведет преподаватель. Для STUDENT поле sheet всегда заполнено и содержит данные о записи; для TEACHER поле sheet всегда null. По умолчанию - все статусы."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список моих занятий"),
            @ApiResponse(responseCode = "400", description = "Невалидные параметры (to <= from)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Нет роли STUDENT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<Page<MyLessonResponse>> getMy(
            @Parameter(description = "Статусы по времени относительно now. По умолчанию — все статусы.")
            @RequestParam(required = false) List<LessonTimeStatus> status,
            @Parameter(description = "Фильтр по посещению (для PAST). null — оба.")
            @RequestParam(required = false) Boolean visited,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                lessonService.getMyLessons(status, visited, from, to, PageRequest.of(page, size), principal)
        );
    }

    @Operation(summary = "Детали одного занятия", description = "Отдаёт любой урок по id, включая CANCELLED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Детали урока"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Отсутствует авторизация",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Урок не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<LessonResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(lessonService.getById(id));
    }

    @Operation(
            summary = "Создать занятие",
            description = "Teacher создаёт урок в своём кампусе и автоматически становится его преподавателем (teacherId в body игнорируется / должен совпадать с собой). Admin создаёт с явным teacherId; кампус преподавателя должен совпадать с campusId урока."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Урок создан"),
            @ApiResponse(responseCode = "400", description = "Невалидный payload / чужой кампус / teacherId не задан (admin) / teacher из другого кампуса / workoutType неактивен / endTime ≤ startTime",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Нет роли TEACHER/ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "У преподавателя уже есть ACTIVE урок в это время",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<LessonResponse> create(
            @RequestBody @Valid LessonRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.create(request, principal));
    }

    @Operation(
            summary = "Создать регулярные занятия по расписанию",
            description = "Генерирует серию уроков между startDate и endDate по указанным дням недели. Максимум 365 дней между датами. Каждый урок проверяется на пересечение со временем преподавателя."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Серия создана (возвращает count + firstDate/lastDate)"),
            @ApiResponse(responseCode = "400", description = "endDate < startDate, диапазон > 365 дней, endTime ≤ startTime, пустой daysOfWeek, чужой кампус, workoutType не найден/неактивен, teacher не тот",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Нет роли TEACHER/ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Хотя бы один из сгенерированных уроков пересекается с существующим у преподавателя",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/recurring")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<RecurringLessonResponse> createRecurring(
            @RequestBody @Valid RecurringLessonRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.createRecurring(request, principal));
    }

    @Operation(
            summary = "Частичное обновление урока",
            description = "Teacher правит только свои уроки. Admin — любые. Менять можно любые поля. При смене teacherId — проверка, что новый учитель того же кампуса. Все null-поля игнорируются."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Урок обновлён"),
            @ApiResponse(responseCode = "400", description = "Teacher правит чужой урок / workoutType неактивен / campus недоступен / teacher из другого кампуса / endTime ≤ startTime",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Нет роли TEACHER/ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Урок не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Урок CANCELLED (редактировать нельзя) ИЛИ конфликт по времени у преподавателя",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<LessonResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid LessonPatchRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(lessonService.update(id, request, principal));
    }

    @Operation(
            summary = "Отменить урок",
            description = "Ставит статус CANCELLED. Отмена необратима через API. Записанные студенты остаются в списке, но не смогут больше записываться/отписываться (кроме совсем редких случаев, см. /sheets)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Урок отменён"),
            @ApiResponse(responseCode = "400", description = "Урок уже прошёл (endTime <= now) ИЛИ teacher отменяет чужой",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Нет роли TEACHER/ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Урок не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Урок уже отменён",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<LessonResponse> cancel(
            @PathVariable UUID id,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(lessonService.cancel(id, principal));
    }
}
