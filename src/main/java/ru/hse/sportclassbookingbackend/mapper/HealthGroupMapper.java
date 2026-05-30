package ru.hse.sportclassbookingbackend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupRequest;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupResponse;
import ru.hse.sportclassbookingbackend.model.HealthGroup;

@Mapper(componentModel = "spring")
public interface HealthGroupMapper {

    HealthGroupResponse toResponse(HealthGroup healthGroup);

    void updateFromPut(HealthGroupRequest request, @MappingTarget HealthGroup group);
}
