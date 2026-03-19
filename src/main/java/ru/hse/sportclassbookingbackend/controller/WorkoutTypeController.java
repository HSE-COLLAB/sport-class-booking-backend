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
import ru.hse.sportclassbookingbackend.dto.workout_type.WorkoutTypePatchRequest;
import ru.hse.sportclassbookingbackend.dto.workout_type.WorkoutTypeRequest;
import ru.hse.sportclassbookingbackend.dto.workout_type.WorkoutTypeResponse;
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
        List<WorkoutTypeResponse> workoutTypeResponses = workoutTypeService.getAll();
        return ResponseEntity.ok(workoutTypeResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkoutTypeResponse> getById(@PathVariable UUID id) {
        WorkoutTypeResponse workoutTypeResponse = workoutTypeService.getById(id);
        return ResponseEntity.ok(workoutTypeResponse);
    }

    @PostMapping
    public ResponseEntity<WorkoutTypeResponse> create(@RequestBody @Valid WorkoutTypeRequest request) {
        WorkoutTypeResponse workoutTypeResponse = workoutTypeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutTypeResponse);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WorkoutTypeResponse> patch(@PathVariable UUID id, @RequestBody WorkoutTypePatchRequest request) {
        WorkoutTypeResponse workoutTypeResponse = workoutTypeService.patch(id, request);
        return ResponseEntity.ok(workoutTypeResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        workoutTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
