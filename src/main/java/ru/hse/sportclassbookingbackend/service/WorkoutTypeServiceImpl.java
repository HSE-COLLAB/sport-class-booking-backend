package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hse.sportclassbookingbackend.dto.workout_type.WorkoutTypePatchRequest;
import ru.hse.sportclassbookingbackend.dto.workout_type.WorkoutTypeRequest;
import ru.hse.sportclassbookingbackend.dto.workout_type.WorkoutTypeResponse;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.exception.ValidationException;
import ru.hse.sportclassbookingbackend.mapper.WorkoutTypeMapper;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.model.WorkoutType;
import ru.hse.sportclassbookingbackend.repository.HealthGroupRepository;
import ru.hse.sportclassbookingbackend.repository.WorkoutTypeRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutTypeServiceImpl implements WorkoutTypeService {
    private final WorkoutTypeRepository workoutTypeRepository;
    private final HealthGroupRepository healthGroupRepository;
    private final WorkoutTypeMapper workoutTypeMapper;

    public List<WorkoutTypeResponse> getAll() {
        return workoutTypeRepository.findAllByIsActiveTrue().stream().map(workoutTypeMapper::toResponse).toList();
    }

    public WorkoutTypeResponse getById(UUID id) {
        WorkoutType workoutType = workoutTypeRepository.findByIdAndIsActiveTrue(id).orElseThrow(() -> new NotFoundException("Workout type with such ID wasn't found"));
        return workoutTypeMapper.toResponse(workoutType);
    }

    public WorkoutTypeResponse create(WorkoutTypeRequest request) {
        HealthGroup healthGroup = healthGroupRepository.findById(request.allowHealthGroupId()).orElseThrow(() -> new ValidationException("Health group wasn't found"));
        WorkoutType workoutType = workoutTypeMapper.toEntity(request);
        workoutType.setAllowHealthGroup(healthGroup);
        workoutType.setIsActive(true);
        return workoutTypeMapper.toResponse(workoutTypeRepository.save(workoutType));
    }

    public WorkoutTypeResponse patch(UUID id, WorkoutTypePatchRequest request) {
        WorkoutType workoutType = workoutTypeRepository.findByIdAndIsActiveTrue(id).orElseThrow(() -> new NotFoundException("Workout type wasn't found"));

        if (request.title() != null) workoutType.setTitle(request.title());
        if (request.allowHealthGroupId() != null) {
            HealthGroup healthGroup = healthGroupRepository.findById(request.allowHealthGroupId()).orElseThrow(() -> new ValidationException("Health group wasn't found"));
            workoutType.setAllowHealthGroup(healthGroup);
        }

        return workoutTypeMapper.toResponse(workoutTypeRepository.save(workoutType));
    }

    public void delete(UUID id) {
        Optional<WorkoutType> workoutType = workoutTypeRepository.findById(id);
        if (workoutType.isPresent()) {
            workoutType.get().setIsActive(false);
            workoutTypeRepository.save(workoutType.get());
        }
    }
}
