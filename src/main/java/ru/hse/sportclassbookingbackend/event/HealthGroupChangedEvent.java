package ru.hse.sportclassbookingbackend.event;

import java.util.UUID;

public record HealthGroupChangedEvent(
        UUID studentId,
        Integer oldGroupId,
        Integer newGroupId
) {
}
