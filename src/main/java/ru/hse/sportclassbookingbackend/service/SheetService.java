package ru.hse.sportclassbookingbackend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonTimeStatus;
import ru.hse.sportclassbookingbackend.dto.sheet.AttendeeResponse;
import ru.hse.sportclassbookingbackend.dto.sheet.BulkAttendanceRequest;
import ru.hse.sportclassbookingbackend.dto.sheet.MyLessonResponse;
import ru.hse.sportclassbookingbackend.dto.sheet.SheetIdResponse;
import ru.hse.sportclassbookingbackend.security.UserPrincipal;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SheetService {

    SheetIdResponse register(UUID lessonId, UserPrincipal principal);

    void cancel(UUID lessonId, UUID sheetId, UserPrincipal principal);

    List<AttendeeResponse> getAttendees(UUID lessonId);

    List<AttendeeResponse> markAttendance(UUID lessonId, BulkAttendanceRequest request, UserPrincipal principal);

    Page<MyLessonResponse> getMyLessons(Collection<LessonTimeStatus> timeStatuses, Boolean visited, LocalDateTime from,
                                        LocalDateTime to, Pageable pageable, UserPrincipal principal);
}
