package ru.hse.sportclassbookingbackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hse.sportclassbookingbackend.model.Lesson;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    @Query("""
            SELECT l FROM Lesson l
            JOIN FETCH l.workoutType wt
            JOIN FETCH wt.allowHealthGroup
            JOIN FETCH l.teacher t
            WHERE (:workoutTypeId IS NULL OR l.workoutType.id = :workoutTypeId)
            AND (:teacherId IS NULL OR l.teacher.id = :teacherId)
            AND (:from IS NULL OR l.startTime >= :from)
            AND (:to IS NULL OR l.startTime <= :to)
            AND (:place IS NULL OR LOWER(l.place) LIKE LOWER(CONCAT('%', :place, '%')))
            AND (
                :status = 'UPCOMING' AND l.startTime > :now
                OR :status = 'ONGOING' AND l.startTime <= :now AND l.endTime >= :now
                OR :status = 'PAST' AND l.endTime < :now
                OR :status IS NULL
            )
            """)
    Page<Lesson> findAllWithFilters(
            @Param("workoutTypeId") UUID workoutTypeId,
            @Param("teacherId") UUID teacherId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("place") String place,
            @Param("status") String status,
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );
}
