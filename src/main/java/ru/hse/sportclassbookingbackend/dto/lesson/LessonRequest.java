package ru.hse.sportclassbookingbackend.dto.lesson;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LessonRequest(
        @NotBlank
        String title,
        @NotBlank
        String place,
        @NotNull @Future
        OffsetDateTime startTime,
        @NotNull
        OffsetDateTime endTime,
        @NotNull @Positive
        Integer totalPlaces,
        @NotNull
        UUID workoutTypeId,
        UUID teacherId,
        String notes
) {
}
