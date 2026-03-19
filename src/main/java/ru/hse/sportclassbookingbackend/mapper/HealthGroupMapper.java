package ru.hse.sportclassbookingbackend.mapper;

import org.mapstruct.Mapper;
import ru.hse.sportclassbookingbackend.dto.workouttype.HealthGroupResponse;
import ru.hse.sportclassbookingbackend.model.HealthGroup;

@Mapper(componentModel="spring")
public interface HealthGroupMapper {
    HealthGroupResponse toResponse(HealthGroup healthGroup);
}
