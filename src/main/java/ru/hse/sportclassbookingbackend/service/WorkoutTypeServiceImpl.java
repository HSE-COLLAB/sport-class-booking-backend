package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypePatchRequest;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeRequest;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeResponse;
import ru.hse.sportclassbookingbackend.exception.BadRequestException;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.WorkoutTypeMapper;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.model.WorkoutType;
import ru.hse.sportclassbookingbackend.repository.HealthGroupRepository;
import ru.hse.sportclassbookingbackend.repository.WorkoutTypeRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutTypeServiceImpl implements WorkoutTypeService {

    private final WorkoutTypeRepository workoutTypeRepository;

    private final HealthGroupRepository healthGroupRepository;

    private final WorkoutTypeMapper workoutTypeMapper;

    public List<WorkoutTypeResponse> getAll() {
        return workoutTypeRepository.findAllByIsActiveTrue()
                .stream()
                .map(workoutTypeMapper::toResponse)
                .toList();
    }

    public WorkoutTypeResponse getById(UUID id) {
        WorkoutType workoutType = workoutTypeRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Workout type with id: " + id + " was not found"));

        return workoutTypeMapper.toResponse(workoutType);
    }

    public WorkoutTypeResponse create(WorkoutTypeRequest request) {
        HealthGroup healthGroup = healthGroupRepository.findById(request.allowHealthGroupId())
                .orElseThrow(() -> new BadRequestException("Health group with id: " + request.allowHealthGroupId() + " was not found"));

        WorkoutType workoutType = workoutTypeMapper.toEntity(request);
        workoutType.setAllowHealthGroup(healthGroup);
        workoutType.setIsActive(true);

        return workoutTypeMapper.toResponse(workoutTypeRepository.save(workoutType));
    }

    public WorkoutTypeResponse update(UUID id, WorkoutTypePatchRequest request) {
        WorkoutType workoutType = workoutTypeRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Workout type with id: " + id + " was not found"));

        if (request.title() != null) {
            workoutType.setTitle(request.title());
        }
        if (request.allowHealthGroupId() != null) {
            HealthGroup healthGroup = healthGroupRepository.findById(request.allowHealthGroupId())
                    .orElseThrow(() -> new BadRequestException("Health group with id: " + request.allowHealthGroupId() + " was not found"));
            workoutType.setAllowHealthGroup(healthGroup);
        }

        return workoutTypeMapper.toResponse(workoutTypeRepository.save(workoutType));
    }

    public void delete(UUID id) {
        workoutTypeRepository.findByIdAndIsActiveTrue(id).ifPresent(wt -> {
            wt.setIsActive(false);
            workoutTypeRepository.save(wt);
        });
    }
}
