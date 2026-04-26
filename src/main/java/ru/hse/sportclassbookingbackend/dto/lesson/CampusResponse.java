package ru.hse.sportclassbookingbackend.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;

public record CampusResponse(
        @Schema(example = "3")
        Integer id,
        @Schema(example = "Нижний Новгород")
        String name,
        @Schema(description = "IANA timezone кампуса, используется для локального отображения времени уроков",
                example = "Europe/Moscow")
        String timezone
) {
}
