package ru.hse.sportclassbookingbackend.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.hse.sportclassbookingbackend.dto.studentgroup.StudentGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.studentgroup.StudentGroupRequest;
import ru.hse.sportclassbookingbackend.dto.studentgroup.StudentGroupResponse;
import ru.hse.sportclassbookingbackend.model.StudentGroup;

@Mapper(componentModel = "spring")
public interface StudentGroupMapper {
    StudentGroupResponse toResponse(StudentGroup group);

    @Mapping(target = "id", ignore = true)
    StudentGroup toEntity(StudentGroupRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromPatch(StudentGroupPatchRequest request, @MappingTarget StudentGroup group);
}
