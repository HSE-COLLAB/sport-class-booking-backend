package ru.hse.sportclassbookingbackend.controller;

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
import ru.hse.sportclassbookingbackend.security.UserPrincipal;
import ru.hse.sportclassbookingbackend.service.SheetService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/lessons/{lessonId}/sheets")
@RequiredArgsConstructor
public class SheetController {

    private final SheetService sheetService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SheetIdResponse> register(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sheetService.register(lessonId, principal));
    }

    @DeleteMapping("/{sheetId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID lessonId,
            @PathVariable UUID sheetId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        sheetService.cancel(lessonId, sheetId, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<AttendeeResponse>> getAttendees(@PathVariable UUID lessonId) {
        return ResponseEntity.ok(sheetService.getAttendees(lessonId));
    }

    @PatchMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<AttendeeResponse>> markAttendance(
            @PathVariable UUID lessonId,
            @RequestBody @Valid BulkAttendanceRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(sheetService.markAttendance(lessonId, request, principal));
    }
}
