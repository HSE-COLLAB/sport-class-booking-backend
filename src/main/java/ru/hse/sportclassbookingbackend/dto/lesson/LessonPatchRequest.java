package ru.hse.sportclassbookingbackend.dto.lesson;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.UUID;

public record LessonPatchRequest(
        @Pattern(regexp = "^(?!\\s*$).+", message = "title must not be blank")
        String title,
        @Pattern(regexp = "^(?!\\s*$).+", message = "place must not be blank")
        String place,
        @Future
        LocalDateTime startTime,
        @Future
        LocalDateTime endTime,
        @Positive
        Integer totalPlaces,
        UUID workoutTypeId,
        Integer campusId,
        UUID teacherId,
        String notes
) {
}
