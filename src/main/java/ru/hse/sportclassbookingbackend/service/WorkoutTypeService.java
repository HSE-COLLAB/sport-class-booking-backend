package ru.hse.sportclassbookingbackend.service;

import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypePatchRequest;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeRequest;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeResponse;

import java.util.List;
import java.util.UUID;

public interface WorkoutTypeService {

    List<WorkoutTypeResponse> getAll();

    WorkoutTypeResponse getById(UUID id);

    WorkoutTypeResponse create(WorkoutTypeRequest request);

    WorkoutTypeResponse update(UUID id, WorkoutTypePatchRequest request);

    void delete(UUID id);
}
