package ru.hse.sportclassbookingbackend.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Частичное обновление урока. Все поля опциональны — null игнорируется.")
public record LessonPatchRequest(
        @Pattern(regexp = "^(?!\\s*$).+", message = "title must not be blank")
        String title,
        @Pattern(regexp = "^(?!\\s*$).+", message = "place must not be blank")
        String place,
        @Future
        @Schema(example = "2099-07-01T10:00:00")
        LocalDateTime startTime,
        @Future
        @Schema(example = "2099-07-01T11:30:00")
        LocalDateTime endTime,
        @Positive
        Integer totalPlaces,
        @Schema(description = "ID типа тренировки из GET /workout-types")
        UUID workoutTypeId,
        @Schema(description = "ID кампуса")
        Integer campusId,
        @Schema(description = "ID преподавателя. TEACHER менять не может, ADMIN — может. Новый учитель должен быть того же кампуса.")
        UUID teacherId,
        String notes
) {
}
