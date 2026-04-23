package ru.hse.sportclassbookingbackend.controller;

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
import ru.hse.sportclassbookingbackend.security.UserPrincipal;
import ru.hse.sportclassbookingbackend.service.LessonService;
import ru.hse.sportclassbookingbackend.service.SheetService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final SheetService sheetService;

    @GetMapping
    public ResponseEntity<Page<LessonResponse>> getAll(
            @RequestParam Integer campusId,
            @RequestParam(required = false) List<UUID> workoutTypeId,
            @RequestParam(required = false) UUID teacherId,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(required = false) String place,
            @RequestParam(required = false) Boolean myHealthGroup,
            @RequestParam(required = false) List<LessonTimeStatus> status,
            @RequestParam(required = false) Boolean includeCancelled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                lessonService.getAll(campusId, workoutTypeId, teacherId, from, to, place, myHealthGroup, status,
                        includeCancelled, PageRequest.of(page, size), principal)
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<MyLessonResponse>> getMy(
            @RequestParam(required = false) LessonTimeStatus status,
            @RequestParam(required = false) Boolean visited,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                sheetService.getMyLessons(status, visited, from, to, PageRequest.of(page, size), principal)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<LessonResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(lessonService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<LessonResponse> create(
            @RequestBody @Valid LessonRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.create(request, principal));
    }

    @PostMapping("/recurring")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<RecurringLessonResponse> createRecurring(
            @RequestBody @Valid RecurringLessonRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.createRecurring(request, principal));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<LessonResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid LessonPatchRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(lessonService.update(id, request, principal));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<LessonResponse> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(lessonService.cancel(id, principal));
    }
}
