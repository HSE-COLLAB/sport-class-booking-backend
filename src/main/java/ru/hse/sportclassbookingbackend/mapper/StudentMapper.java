package ru.hse.sportclassbookingbackend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.hse.sportclassbookingbackend.dto.RegisterStudentRequest;
import ru.hse.sportclassbookingbackend.model.Student;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "sheets", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "healthGroup", ignore = true)
    Student toEntity(RegisterStudentRequest req);
}
