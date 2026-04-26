package ru.hse.sportclassbookingbackend.handler;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Стандартный формат ошибок. Для 4xx — сообщения из BadRequest/NotFound/Unauthorized/Conflict, для валидационных — список ошибок по полям.")
public record ErrorResponse(
        @Schema(example = "[\"email: must be a well-formed email address\"]")
        List<String> errors
) {
    public static ErrorResponse of(String message) {
        return new ErrorResponse(List.of(message));
    }
}
