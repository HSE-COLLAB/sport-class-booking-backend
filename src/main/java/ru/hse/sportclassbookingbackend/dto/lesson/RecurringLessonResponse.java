package ru.hse.sportclassbookingbackend.dto.lesson;

import java.time.LocalDate;

public record RecurringLessonResponse(
        int count,
        LocalDate firstDate,
        LocalDate lastDate
) {
}
