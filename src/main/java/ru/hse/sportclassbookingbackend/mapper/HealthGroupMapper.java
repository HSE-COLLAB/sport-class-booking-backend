package ru.hse.sportclassbookingbackend.mapper;

import org.mapstruct.Mapper;
import ru.hse.sportclassbookingbackend.dto.workout_type.HealthGroupResponse;
import ru.hse.sportclassbookingbackend.model.HealthGroup;

@Mapper(componentModel="spring")
public interface HealthGroupMapper {
    HealthGroupResponse toResponse(HealthGroup healthGroup);
}
