package ru.hse.sportclassbookingbackend.service;

import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupRequest;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupResponse;
import ru.hse.sportclassbookingbackend.repository.HealthGroupRepository;

import java.util.List;

public interface HealthGroupService {

    List<HealthGroupResponse> getAll();

    HealthGroupResponse create(HealthGroupRequest request);

    HealthGroupResponse update(int id, HealthGroupPatchRequest request);

    void delete(int id);
}
