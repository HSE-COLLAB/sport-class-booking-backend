package ru.hse.sportclassbookingbackend.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.UUID;

public record LessonRequest(
        @NotBlank
        @Schema(example = "Волейбол утро")
        String title,
        @NotBlank
        @Schema(description = "Место проведения", example = "Зал 1")
        String place,
        @NotNull @Future
        @Schema(description = "Время начала (локальное время кампуса, ISO без TZ). Сервер переведёт в UTC по timezone кампуса.",
                example = "2099-07-01T10:00:00")
        LocalDateTime startTime,
        @NotNull @Future
        @Schema(description = "Время окончания. Должно быть > startTime.", example = "2099-07-01T11:30:00")
        LocalDateTime endTime,
        @NotNull @Positive
        @Schema(description = "Максимальное количество мест на занятии", example = "15")
        Integer totalPlaces,
        @Schema(description = "ID типа тренировки из GET /workout-types", example = "10000000-0000-0000-0000-000000000001")
        @NotNull
        UUID workoutTypeId,
        @Schema(description = "ID кампуса (1=Москва, 2=СПб, 3=НН, 4=Пермь)", example = "3")
        @NotNull
        Integer campusId,
        @Schema(description = "ID преподавателя. Обязателен для ADMIN, для TEACHER игнорируется (подставляется самим учителем из токена).")
        UUID teacherId,
        @Schema(description = "Произвольные заметки, видимые студентам")
        String notes
) {
}
