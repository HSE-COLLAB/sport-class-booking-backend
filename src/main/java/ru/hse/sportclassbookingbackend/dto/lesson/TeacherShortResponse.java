package ru.hse.sportclassbookingbackend.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Краткая информация о преподавателе в составе урока.")
public record TeacherShortResponse(
        @Schema(example = "33333333-3333-3333-3333-333333333333")
        UUID id,
        String firstName,
        String lastName,
        String middleName,
        String position
) {
}
