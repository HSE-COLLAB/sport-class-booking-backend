package ru.hse.sportclassbookingbackend.dto.sheet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Краткая инфа о студенте — используется в списке записавшихся.")
public record StudentShortResponse(
        @Schema(example = "55555555-5555-5555-5555-555555555555")
        UUID id,
        String firstName,
        String lastName,
        String middleName,
        @Schema(description = "Номер учебной группы (не UUID, а человекочитаемый, например 'ПИ-2024')")
        String group,
        @Schema(description = "Описание медгруппы", example = "Основная группа")
        String healthGroup
) {
}
