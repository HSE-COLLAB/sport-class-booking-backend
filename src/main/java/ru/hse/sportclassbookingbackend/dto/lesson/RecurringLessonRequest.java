package ru.hse.sportclassbookingbackend.dto.lesson;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record RecurringLessonRequest(
        @NotBlank
        String title,
        @NotBlank
        String place,
        @NotNull
        LocalTime startTime,
        @NotNull
        LocalTime endTime,
        @NotNull @Positive
        Integer totalPlaces,
        @NotNull
        UUID workoutTypeId,
        UUID teacherId,
        String notes,
        @NotEmpty
        Set<DayOfWeek> daysOfWeek,
        @NotNull @Future
        LocalDate startDate,
        @NotNull @Future
        LocalDate endDate
) {
}
