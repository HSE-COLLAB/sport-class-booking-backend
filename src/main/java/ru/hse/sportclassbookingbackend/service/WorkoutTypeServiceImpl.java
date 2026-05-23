package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypePatchRequest;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeRequest;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeResponse;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.WorkoutTypeMapper;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.model.WorkoutType;
import ru.hse.sportclassbookingbackend.repository.HealthGroupRepository;
import ru.hse.sportclassbookingbackend.repository.WorkoutTypeRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutTypeServiceImpl implements WorkoutTypeService {

    private final WorkoutTypeRepository workoutTypeRepository;

    private final HealthGroupRepository healthGroupRepository;

    private final WorkoutTypeMapper workoutTypeMapper;

    @Transactional(readOnly = true)
    public List<WorkoutTypeResponse> getAll() {
        return workoutTypeRepository.findAllByIsActiveTrue()
                .stream()
                .map(workoutTypeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkoutTypeResponse getById(UUID id) {
        WorkoutType workoutType = workoutTypeRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Workout type with id: " + id + " was not found"));

        return workoutTypeMapper.toResponse(workoutType);
    }

    @Transactional
    public WorkoutTypeResponse create(WorkoutTypeRequest request) {
        WorkoutType workoutType = workoutTypeMapper.toEntity(request);
        workoutType.setAllowedHealthGroups(findHealthGroupsOrThrow(request.allowedHealthGroupIds()));
        workoutType.setIsActive(true);

        return workoutTypeMapper.toResponse(workoutTypeRepository.save(workoutType));
    }

    @Transactional
    public WorkoutTypeResponse update(UUID id, WorkoutTypePatchRequest request) {
        WorkoutType workoutType = workoutTypeRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Workout type with id: " + id + " was not found"));

        if (request.title() != null) {
            workoutType.setTitle(request.title());
        }
        if (request.allowedHealthGroupIds() != null) {
            workoutType.setAllowedHealthGroups(findHealthGroupsOrThrow(request.allowedHealthGroupIds()));
        }

        return workoutTypeMapper.toResponse(workoutTypeRepository.save(workoutType));
    }

    public void delete(UUID id) {
        workoutTypeRepository.findByIdAndIsActiveTrue(id).ifPresent(wt -> {
            wt.setIsActive(false);
            workoutTypeRepository.save(wt);
        });
    }

    private Set<HealthGroup> findHealthGroupsOrThrow(Set<Integer> ids) {
        List<HealthGroup> found = healthGroupRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new NotFoundException("One or more health group ids not found");
        }
        return new HashSet<>(found);
    }
}
