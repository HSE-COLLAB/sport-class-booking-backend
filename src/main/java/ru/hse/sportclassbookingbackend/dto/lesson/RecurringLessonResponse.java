package ru.hse.sportclassbookingbackend.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record RecurringLessonResponse(
        @Schema(description = "Количество созданных занятий", example = "52")
        int count,
        @Schema(description = "Дата первого занятия", example = "2099-09-01")
        LocalDate firstDate,
        @Schema(description = "Дата последнего занятия", example = "2099-12-23")
        LocalDate lastDate
) {
}
