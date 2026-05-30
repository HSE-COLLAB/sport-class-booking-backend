package ru.hse.sportclassbookingbackend.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import ru.hse.sportclassbookingbackend.dto.lesson.CampusResponse;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonPatchRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonResponse;
import ru.hse.sportclassbookingbackend.dto.lesson.RecurringLessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.TeacherShortResponse;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeResponse;
import ru.hse.sportclassbookingbackend.model.Campus;
import ru.hse.sportclassbookingbackend.model.Lesson;
import ru.hse.sportclassbookingbackend.model.Teacher;
import ru.hse.sportclassbookingbackend.model.WorkoutType;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workoutType", ignore = true)
    @Mapping(target = "teacher", ignore = true)
    @Mapping(target = "campus", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "sheets", ignore = true)
    @Mapping(target = "startTime", ignore = true)
    @Mapping(target = "endTime", ignore = true)
    Lesson toEntity(LessonRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workoutType", ignore = true)
    @Mapping(target = "teacher", ignore = true)
    @Mapping(target = "campus", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "sheets", ignore = true)
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "endTime", source = "endTime")
    Lesson toEntityFromRecurring(RecurringLessonRequest request, OffsetDateTime startTime, OffsetDateTime endTime);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
                 unmappedSourcePolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workoutType", ignore = true)
    @Mapping(target = "teacher", ignore = true)
    @Mapping(target = "campus", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "sheets", ignore = true)
    @Mapping(target = "startTime", ignore = true)
    @Mapping(target = "endTime", ignore = true)
    void updateFromPatch(LessonPatchRequest request, @MappingTarget Lesson lesson);

    TeacherShortResponse toTeacherShortResponse(Teacher teacher);

    CampusResponse toCampusResponse(Campus campus);

    WorkoutTypeResponse toWorkoutTypeResponse(WorkoutType workoutType);
}
