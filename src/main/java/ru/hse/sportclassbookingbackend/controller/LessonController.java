package ru.hse.sportclassbookingbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonPatchRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonResponse;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonStatus;
import ru.hse.sportclassbookingbackend.dto.lesson.RecurringLessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.RecurringLessonResponse;
import ru.hse.sportclassbookingbackend.security.UserPrincipal;
import ru.hse.sportclassbookingbackend.service.LessonService;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @GetMapping
    public ResponseEntity<Page<LessonResponse>> getAll(
            @RequestParam(required = false) UUID workoutTypeId,
            @RequestParam(required = false) UUID teacherId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(required = false) String place,
            @RequestParam(required = false) LessonStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                lessonService.getAll(workoutTypeId, teacherId, from, to, place, status, PageRequest.of(page, size))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<LessonResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(lessonService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<LessonResponse> create(
            @RequestBody @Valid LessonRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.create(request, principal));
    }

    @PostMapping("/recurring")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<RecurringLessonResponse> createRecurring(
            @RequestBody @Valid RecurringLessonRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.createRecurring(request, principal));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<LessonResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid LessonPatchRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(lessonService.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        lessonService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }
}
