package ru.hse.sportclassbookingbackend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterStudentRequest;
import ru.hse.sportclassbookingbackend.dto.student.StudentResponse;
import ru.hse.sportclassbookingbackend.dto.student.StudentPatchRequest;
import ru.hse.sportclassbookingbackend.dto.student.StudentSelfUpdateRequest;
import ru.hse.sportclassbookingbackend.model.Student;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StudentMapper {

    @Mapping(target = "email", source = "email")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "middleName", source = "middleName")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "healthGroupId", source = "healthGroup.id")
    StudentResponse toResponse(Student student);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "sheets", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "healthGroup", ignore = true)
    @Mapping(target = "campus", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    Student toEntity(RegisterStudentRequest request);


    @Mapping(target = "email", source = "email")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "middleName", source = "middleName")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "group", source = "groupId", ignore = true)
    @Mapping(target = "healthGroup", source = "healthGroupId", ignore = true)
    void toStudentFromDto(StudentPatchRequest request, @MappingTarget Student student);

    @Mapping(target = "email", source = "email")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "middleName", source = "middleName")
    void toStudentFromSelfUpdate(StudentSelfUpdateRequest request, @MappingTarget Student student);
}
