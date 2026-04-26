package ru.hse.sportclassbookingbackend.dto.sheet;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.hse.sportclassbookingbackend.dto.lesson.CampusResponse;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonStatus;
import ru.hse.sportclassbookingbackend.dto.lesson.TeacherShortResponse;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Урок в списке «мои занятия» — то же что LessonResponse + вложенная инфа о моей записи.")
public record MyLessonResponse(
        @Schema(example = "20000000-0000-0000-0000-000000000001")
        UUID id,
        String title,
        String place,
        @Schema(example = "2026-05-05T10:00:00+03:00")
        OffsetDateTime startTime,
        @Schema(example = "2026-05-05T11:30:00+03:00")
        OffsetDateTime endTime,
        Integer totalPlaces,
        @Schema(description = "Свободные места")
        Integer availablePlaces,
        LessonStatus status,
        String notes,
        WorkoutTypeResponse workoutType,
        TeacherShortResponse teacher,
        CampusResponse campus,
        @Schema(description = "Данные моей записи на этот урок")
        MySheetInfo sheet
) {
    @Schema(description = "Кусочек Sheet, относящийся к моей записи на этот урок.")
    public record MySheetInfo(
            @Schema(description = "ID моей записи", example = "30000000-0000-0000-0000-000000000001")
            UUID id,
            @Schema(description = "Отметка посещения (null — ещё не отмечено)")
            Boolean visited
    ) {
    }
}
