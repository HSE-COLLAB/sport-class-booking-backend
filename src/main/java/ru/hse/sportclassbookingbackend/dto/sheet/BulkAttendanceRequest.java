package ru.hse.sportclassbookingbackend.dto.sheet;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Массовая отметка посещаемости — список marks.")
public record BulkAttendanceRequest(
        @NotEmpty
        @Valid
        List<AttendanceMark> marks
) {
}
