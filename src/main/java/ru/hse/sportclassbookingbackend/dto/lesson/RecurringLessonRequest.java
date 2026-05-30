package ru.hse.sportclassbookingbackend.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
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

@Schema(description = "Генератор регулярных уроков: один и тот же слот startTime-endTime в указанные дни недели между startDate и endDate.")
public record RecurringLessonRequest(
        @NotBlank
        String title,
        @NotBlank
        String place,
        @Schema(description = "Время начала занятия (HH:mm)", example = "10:00")
        @NotNull
        LocalTime startTime,
        @Schema(description = "Время окончания занятия (HH:mm). Должно быть > startTime.", example = "11:30")
        @NotNull
        LocalTime endTime,
        @NotNull @Positive
        Integer totalPlaces,
        @Schema(description = "ID типа тренировки из GET /workout-types", example = "10000000-0000-0000-0000-000000000001")
        @NotNull
        UUID workoutTypeId,
        String notes,
        @Schema(description = "Дни недели, в которые генерировать уроки", example = "[\"MONDAY\",\"WEDNESDAY\",\"FRIDAY\"]")
        @NotEmpty
        Set<DayOfWeek> daysOfWeek,
        @Schema(description = "Дата начала серии (inclusive)", example = "2099-09-01")
        @NotNull @Future
        LocalDate startDate,
        @Schema(description = "Дата окончания серии (inclusive). Диапазон до 365 дней.", example = "2099-12-25")
        @NotNull @Future
        LocalDate endDate,
        @Schema(description = "ID кампуса", example = "3")
        @NotNull
        Integer campusId,
        @Schema(description = "ID преподавателя. Обязателен для ADMIN.")
        UUID teacherId
) {
}
