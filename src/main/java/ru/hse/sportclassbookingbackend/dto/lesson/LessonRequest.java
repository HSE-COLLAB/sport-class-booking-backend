package ru.hse.sportclassbookingbackend.dto.lesson;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.UUID;

public record LessonRequest(
        @NotBlank
        String title,
        @NotBlank
        String place,
        @NotNull @Future
        LocalDateTime startTime,
        @NotNull @Future
        LocalDateTime endTime,
        @NotNull @Positive
        Integer totalPlaces,
        @NotNull
        UUID workoutTypeId,
        @NotNull
        Integer campusId,
        UUID teacherId,
        String notes
) {
}
