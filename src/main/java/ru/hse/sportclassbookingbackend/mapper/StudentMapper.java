package ru.hse.sportclassbookingbackend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterStudentRequest;
import ru.hse.sportclassbookingbackend.dto.student.StudentResponse;
import ru.hse.sportclassbookingbackend.model.Student;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    StudentResponse toResponse(Student student);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "sheets", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "healthGroup", ignore = true)
    @Mapping(target = "campus", ignore = true)
    Student toEntity(RegisterStudentRequest req);
}
