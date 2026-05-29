package ru.hse.sportclassbookingbackend.service.mail;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record LessonInfo(
        String title,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String place,
        String campusName,
        String teacherFullName,
        ZoneId zoneId
) {
}
