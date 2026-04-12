package ru.hse.sportclassbookingbackend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonResponse;
import ru.hse.sportclassbookingbackend.dto.lesson.TeacherShortResponse;
import ru.hse.sportclassbookingbackend.model.Lesson;
import ru.hse.sportclassbookingbackend.model.Teacher;

@Mapper(componentModel = "spring", uses = {WorkoutTypeMapper.class})
public interface LessonMapper {

    LessonResponse toResponse(Lesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workoutType", ignore = true)
    @Mapping(target = "teacher", ignore = true)
    @Mapping(target = "sheets", ignore = true)
    Lesson toEntity(LessonRequest request);

    @Mapping(source = "id", target = "id")
    TeacherShortResponse toTeacherShortResponse(Teacher teacher);
}
