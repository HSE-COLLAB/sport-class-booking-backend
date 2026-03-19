package ru.hse.sportclassbookingbackend.service;

import ru.hse.sportclassbookingbackend.dto.workout_type.WorkoutTypePatchRequest;
import ru.hse.sportclassbookingbackend.dto.workout_type.WorkoutTypeRequest;
import ru.hse.sportclassbookingbackend.dto.workout_type.WorkoutTypeResponse;

import java.util.List;
import java.util.UUID;

public interface WorkoutTypeService {
    List<WorkoutTypeResponse> getAll();
    WorkoutTypeResponse getById(UUID id);
    WorkoutTypeResponse create(WorkoutTypeRequest request);
    WorkoutTypeResponse patch(UUID id, WorkoutTypePatchRequest request);
    void delete(UUID id);
}
