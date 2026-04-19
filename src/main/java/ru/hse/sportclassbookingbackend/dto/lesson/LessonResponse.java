package ru.hse.sportclassbookingbackend.dto.lesson;

import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LessonResponse(
        UUID id,
        String title,
        String place,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        Integer totalPlaces,
        String notes,
        WorkoutTypeResponse workoutType,
        TeacherShortResponse teacher,
        CampusResponse campus
) {
}
