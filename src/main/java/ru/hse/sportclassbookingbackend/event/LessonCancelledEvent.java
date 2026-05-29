package ru.hse.sportclassbookingbackend.event;

import ru.hse.sportclassbookingbackend.service.mail.LessonInfo;

import java.util.UUID;

public record LessonCancelledEvent(
        UUID lessonId,
        LessonInfo lesson
) {
}
