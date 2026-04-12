package ru.hse.sportclassbookingbackend.dto.lesson;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LessonPatchRequest(
        @Pattern(regexp = "^(?!\\s*$).+", message = "title must not be blank")
        String title,
        @Pattern(regexp = "^(?!\\s*$).+", message = "place must not be blank")
        String place,
        @Future
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        @Positive
        Integer totalPlaces,
        UUID workoutTypeId,
        UUID teacherId,
        String notes
) {
}
