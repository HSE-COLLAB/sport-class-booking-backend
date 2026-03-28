package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupRequest;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupResponse;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.HealthGroupMapper;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.repository.HealthGroupRepository;

import java.util.List;
@Service
@RequiredArgsConstructor
public class HealthGroupServiceImpl implements HealthGroupService{
    private final HealthGroupRepository healthGroupRepository;
    private final HealthGroupMapper healthGroupMapper;
    
    @Override
    public List<HealthGroupResponse>getAll(){
        return healthGroupRepository.findAll()
                .stream()
                .map(healthGroupMapper::toResponse)
                .toList();
    }
    
    @Override
    public HealthGroupResponse create(HealthGroupRequest request){
        return healthGroupMapper.toResponse(healthGroupRepository.save(healthGroupMapper.toEntity(request)));
    }

    @Override
    public HealthGroupResponse update(int id, HealthGroupPatchRequest request) {
        HealthGroup healthGroup = healthGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Health group with id: " + id + " was not found"));
        healthGroupMapper.updateFromPatch(request, healthGroup);
        healthGroupRepository.save(healthGroup);
        return healthGroupMapper.toResponse(healthGroup);
    }

    @Override
    public void delete(int id) {healthGroupRepository.deleteById(id);}
}
