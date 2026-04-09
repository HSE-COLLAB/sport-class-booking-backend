package ru.hse.sportclassbookingbackend.service;

import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupRequest;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupResponse;

import java.util.List;

public interface HealthGroupService {

    List<HealthGroupResponse> getAll();

    HealthGroupResponse getById(int id);

    HealthGroupResponse update(int id, HealthGroupRequest request);
}
