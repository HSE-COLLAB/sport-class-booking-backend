package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupRequest;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupResponse;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.HealthGroupMapper;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.repository.HealthGroupRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthGroupServiceImpl implements HealthGroupService {

    private final HealthGroupRepository healthGroupRepository;

    private final HealthGroupMapper healthGroupMapper;

    @Override
    public List<HealthGroupResponse> getAll() {
        return healthGroupRepository.findAll()
                .stream()
                .map(healthGroupMapper::toResponse)
                .toList();
    }

    @Override
    public HealthGroupResponse getById(int id) {
        return healthGroupMapper.toResponse(
                healthGroupRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Health group with id: " + id + " was not found"))
        );
    }

    @Override
    public HealthGroupResponse update(int id, HealthGroupRequest request) {
        HealthGroup healthGroup = healthGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Health group with id: " + id + " was not found"));
        healthGroupMapper.updateFromPut(request, healthGroup);
        HealthGroup saved = healthGroupRepository.save(healthGroup);
        log.info("HealthGroup updated: healthGroupId={}", saved.getId());
        return healthGroupMapper.toResponse(saved);
    }
}
