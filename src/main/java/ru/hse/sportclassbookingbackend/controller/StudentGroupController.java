package ru.hse.sportclassbookingbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import ru.hse.sportclassbookingbackend.service.StudentGroupService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student-groups")
@RequiredArgsConstructor
public class StudentGroupController {

    private final StudentGroupService studentGroupService;

    @GetMapping
    public ResponseEntity<List<StudentGroupResponse>> getAll() {
        return ResponseEntity.ok(studentGroupService.getAll());
    }

    // TODO: GET /{id} — вернуть группу со списком студентов
    // Реализовать когда будет готов студентский функционал

    @PostMapping
    public ResponseEntity<StudentGroupResponse> create(@RequestBody @Valid StudentGroupRequest studentGroupRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentGroupService.create(studentGroupRequest));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StudentGroupResponse> update(
            @PathVariable UUID id,
            @RequestBody StudentGroupPatchRequest request
    ) {
        return ResponseEntity.ok(studentGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        studentGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
