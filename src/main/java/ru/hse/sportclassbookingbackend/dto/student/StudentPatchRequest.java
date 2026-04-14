package ru.hse.sportclassbookingbackend.dto.student;

import java.util.UUID;

public record StudentPatchRequest(
        UUID group_id,

        Integer health_group_id
) {
}
