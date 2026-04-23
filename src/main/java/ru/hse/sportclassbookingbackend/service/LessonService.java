package ru.hse.sportclassbookingbackend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonPatchRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonResponse;
import ru.hse.sportclassbookingbackend.dto.lesson.LessonTimeStatus;
import ru.hse.sportclassbookingbackend.dto.lesson.RecurringLessonRequest;
import ru.hse.sportclassbookingbackend.dto.lesson.RecurringLessonResponse;
import ru.hse.sportclassbookingbackend.security.UserPrincipal;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

public interface LessonService {

    Page<LessonResponse> getAll(Integer campusId, Collection<UUID> workoutTypeIds, UUID teacherId,
                                LocalDateTime from, LocalDateTime to, String place,
                                Boolean myHealthGroup, Collection<LessonTimeStatus> timeStatuses,
                                Boolean includeCancelled, Pageable pageable, UserPrincipal principal);

    LessonResponse getById(UUID id);

    LessonResponse create(LessonRequest request, UserPrincipal principal);

    RecurringLessonResponse createRecurring(RecurringLessonRequest request, UserPrincipal principal);

    LessonResponse update(UUID id, LessonPatchRequest request, UserPrincipal principal);

    LessonResponse cancel(UUID id, UserPrincipal principal);
}
