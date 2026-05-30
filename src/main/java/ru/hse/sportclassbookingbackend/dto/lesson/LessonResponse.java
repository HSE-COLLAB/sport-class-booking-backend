package ru.hse.sportclassbookingbackend.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LessonResponse(
        @Schema(example = "20000000-0000-0000-0000-000000000001")
        UUID id,
        String title,
        String place,
        @Schema(description = "Время начала в OffsetDateTime (offset = timezone кампуса)",
                example = "2026-05-05T10:00:00+03:00")
        OffsetDateTime startTime,
        @Schema(description = "Время окончания в OffsetDateTime (offset = timezone кампуса)",
                example = "2026-05-05T11:30:00+03:00")
        OffsetDateTime endTime,
        @Schema(description = "Максимум мест", example = "20")
        Integer totalPlaces,
        @Schema(description = "Свободные места = totalPlaces − количество записавшихся", example = "15")
        Integer availablePlaces,
        LessonStatus status,
        String notes,
        WorkoutTypeResponse workoutType,
        TeacherShortResponse teacher,
        CampusResponse campus
) {
}
