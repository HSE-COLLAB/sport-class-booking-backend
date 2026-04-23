package ru.hse.sportclassbookingbackend.dto.sheet;

import ru.hse.sportclassbookingbackend.dto.lesson.CampusResponse;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonStatus;
import ru.hse.sportclassbookingbackend.dto.lesson.TeacherShortResponse;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MyLessonResponse(
        UUID id,
        String title,
        String place,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        Integer totalPlaces,
        Integer availablePlaces,
        LessonStatus status,
        String notes,
        WorkoutTypeResponse workoutType,
        TeacherShortResponse teacher,
        CampusResponse campus,
        MySheetInfo sheet
) {
    public record MySheetInfo(
            UUID id,
            Boolean visited
    ) {
    }
}
