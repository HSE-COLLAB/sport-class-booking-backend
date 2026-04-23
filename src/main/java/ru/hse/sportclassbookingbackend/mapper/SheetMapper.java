package ru.hse.sportclassbookingbackend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.hse.sportclassbookingbackend.dto.sheet.AttendeeResponse;
import ru.hse.sportclassbookingbackend.dto.sheet.StudentShortResponse;
import ru.hse.sportclassbookingbackend.model.Sheet;
import ru.hse.sportclassbookingbackend.model.Student;

@Mapper(componentModel = "spring")
public interface SheetMapper {

    @Mapping(target = "sheetId", source = "id")
    @Mapping(target = "visited", source = "visited")
    @Mapping(target = "student", source = "student")
    AttendeeResponse toAttendeeResponse(Sheet sheet);

    @Mapping(target = "group", source = "group.groupNumber")
    @Mapping(target = "healthGroup", source = "healthGroup.description")
    StudentShortResponse toStudentShortResponse(Student student);
}
