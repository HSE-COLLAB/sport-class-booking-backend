package ru.hse.sportclassbookingbackend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonPatchRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonResponse;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonStatus;
import ru.hse.sportclassbookingbackend.dto.lesson.RecurringLessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.RecurringLessonResponse;
import ru.hse.sportclassbookingbackend.security.UserPrincipal;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface LessonService {

    Page<LessonResponse> getAll(UUID workoutTypeId, UUID teacherId, Integer campusId, OffsetDateTime from,
                                OffsetDateTime to, String place, LessonStatus status, Pageable pageable);

    LessonResponse getById(UUID id);

    LessonResponse create(LessonRequest request, UserPrincipal principal);

    RecurringLessonResponse createRecurring(RecurringLessonRequest request, UserPrincipal principal);

    LessonResponse update(UUID id, LessonPatchRequest request, UserPrincipal principal);

    void delete(UUID id, UserPrincipal principal);
}
