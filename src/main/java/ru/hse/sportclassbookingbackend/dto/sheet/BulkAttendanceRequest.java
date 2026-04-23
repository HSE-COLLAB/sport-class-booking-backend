package ru.hse.sportclassbookingbackend.dto.sheet;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkAttendanceRequest(
        @NotEmpty
        @Valid
        List<AttendanceMark> marks
) {
}
