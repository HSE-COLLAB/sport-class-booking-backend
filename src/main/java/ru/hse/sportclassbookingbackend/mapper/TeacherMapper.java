package ru.hse.sportclassbookingbackend.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterTeacherRequest;
import ru.hse.sportclassbookingbackend.model.Teacher;

@Mapper(componentModel = "spring")
public interface TeacherMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    Teacher toEntity(RegisterTeacherRequest req);
}
