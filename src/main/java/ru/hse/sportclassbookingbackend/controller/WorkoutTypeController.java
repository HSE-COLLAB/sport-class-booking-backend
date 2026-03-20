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
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypePatchRequest;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeRequest;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeResponse;
import ru.hse.sportclassbookingbackend.service.WorkoutTypeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workout-types")
@RequiredArgsConstructor
public class WorkoutTypeController {

    private final WorkoutTypeService workoutTypeService;

    @GetMapping
    public ResponseEntity<List<WorkoutTypeResponse>> getAll() {
        return ResponseEntity.ok(workoutTypeService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkoutTypeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(workoutTypeService.getById(id));
    }

    @PostMapping
    public ResponseEntity<WorkoutTypeResponse> create(@RequestBody @Valid WorkoutTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutTypeService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WorkoutTypeResponse> update(
            @PathVariable UUID id,
            @RequestBody WorkoutTypePatchRequest request
    ) {
        return ResponseEntity.ok(workoutTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        workoutTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
