package ru.hse.sportclassbookingbackend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hse.sportclassbookingbackend.model.Lesson;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Lesson l WHERE l.id = :id")
    Optional<Lesson> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            SELECT l FROM Lesson l
            JOIN FETCH l.workoutType wt
            JOIN FETCH l.teacher t
            JOIN FETCH l.campus c
            WHERE l.campus.id = :campusId
            AND (:workoutTypeIds IS NULL OR l.workoutType.id IN :workoutTypeIds)
            AND (cast(:teacherId as uuid) IS NULL OR l.teacher.id = :teacherId)
            AND (cast(:from as timestamp) IS NULL OR l.endTime >= :from)
            AND (cast(:to as timestamp) IS NULL OR l.startTime <= :to)
            AND (cast(:place as string) IS NULL OR LOWER(l.place) LIKE LOWER(CONCAT('%', cast(:place as string), '%')))
            AND (cast(:healthGroupId as integer) IS NULL OR EXISTS (
                SELECT 1 FROM HealthGroup hg
                WHERE hg MEMBER OF wt.allowedHealthGroups AND hg.id = :healthGroupId
            ))
            AND (:includeCancelled = TRUE OR l.status = 'ACTIVE')
            AND (
                :timeStatuses IS NULL
                OR ('UPCOMING' IN :timeStatuses AND l.startTime > :now)
                OR ('ONGOING' IN :timeStatuses AND l.startTime <= :now AND l.endTime >= :now)
                OR ('PAST' IN :timeStatuses AND l.endTime < :now)
            )
            """)
    Page<Lesson> findAllWithFilters(
            @Param("campusId") Integer campusId,
            @Param("workoutTypeIds") Collection<UUID> workoutTypeIds,
            @Param("teacherId") UUID teacherId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("place") String place,
            @Param("healthGroupId") Integer healthGroupId,
            @Param("timeStatuses") Collection<String> timeStatuses,
            @Param("includeCancelled") boolean includeCancelled,
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );

    @Query("""
            SELECT CASE WHEN COUNT(l) > 0 THEN TRUE ELSE FALSE END
            FROM Lesson l
            WHERE l.teacher.id = :teacherId
              AND l.status = 'ACTIVE'
              AND (cast(:excludeLessonId as uuid) IS NULL OR l.id <> :excludeLessonId)
              AND l.startTime < :endTime
              AND l.endTime > :startTime
            """)
    boolean hasTeacherTimeOverlap(
            @Param("teacherId") UUID teacherId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("excludeLessonId") UUID excludeLessonId
    );
}
