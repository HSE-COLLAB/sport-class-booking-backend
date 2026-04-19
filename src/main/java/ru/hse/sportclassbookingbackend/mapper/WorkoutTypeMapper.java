package ru.hse.sportclassbookingbackend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeRequest;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeResponse;
import ru.hse.sportclassbookingbackend.model.WorkoutType;

@Mapper(componentModel = "spring", uses = HealthGroupMapper.class)
public interface WorkoutTypeMapper {
    WorkoutTypeResponse toResponse(WorkoutType workoutType);

    @Mapping(target = "allowedHealthGroups", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    WorkoutType toEntity(WorkoutTypeRequest request);
}
