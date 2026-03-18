package ru.hse.sportclassbookingbackend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.hse.sportclassbookingbackend.dto.workout_type.WorkoutTypeRequest;
import ru.hse.sportclassbookingbackend.dto.workout_type.WorkoutTypeResponse;
import ru.hse.sportclassbookingbackend.model.WorkoutType;

@Mapper(componentModel = "spring", uses = HealthGroupMapper.class)
public interface WorkoutTypeMapper {
    @Mapping(target = "allowHealthGroup", source = "allowHealthGroup")
    WorkoutTypeResponse toResponse(WorkoutType workoutType);

    @Mapping(target="allowHealthGroup", ignore = true)
    @Mapping(target="id", ignore = true)
    @Mapping(target="isActive", ignore = true)
    WorkoutType toEntity(WorkoutTypeRequest request);
}
