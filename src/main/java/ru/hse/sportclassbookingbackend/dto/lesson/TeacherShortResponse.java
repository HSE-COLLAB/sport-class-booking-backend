package ru.hse.sportclassbookingbackend.dto.lesson;

import java.util.UUID;

public record TeacherShortResponse(
        UUID id,
        String firstName,
        String lastName,
        String position
) {
}
