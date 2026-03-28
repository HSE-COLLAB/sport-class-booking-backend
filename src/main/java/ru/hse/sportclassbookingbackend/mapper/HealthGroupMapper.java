package ru.hse.sportclassbookingbackend.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupRequest;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupResponse;
import ru.hse.sportclassbookingbackend.model.HealthGroup;

@Mapper(componentModel="spring")
public interface HealthGroupMapper {
    HealthGroupResponse toResponse(HealthGroup healthGroup);

    @Mapping(target = "id", ignore = true)
    HealthGroup toEntity(HealthGroupRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromPatch(HealthGroupPatchRequest request, @MappingTarget HealthGroup group);
}
