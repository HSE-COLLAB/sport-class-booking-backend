package ru.hse.sportclassbookingbackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hse.sportclassbookingbackend.model.Lesson;
import ru.hse.sportclassbookingbackend.model.Sheet;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SheetRepository extends JpaRepository<Sheet, UUID> {

    boolean existsByLessonIdAndStudentId(UUID lessonId, UUID studentId);

    long countByLessonId(UUID lessonId);

    @Query("""
            SELECT s FROM Sheet s
            JOIN FETCH s.student st
            WHERE s.lesson.id = :lessonId
            ORDER BY st.lastName ASC
            """)
    List<Sheet> findAllByLessonIdWithStudent(@Param("lessonId") UUID lessonId);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END
            FROM Sheet s
            WHERE s.student.id = :studentId
              AND s.lesson.status = 'ACTIVE'
              AND s.lesson.startTime < :endTime
              AND s.lesson.endTime > :startTime
            """)
    boolean hasTimeOverlap(@Param("studentId") UUID studentId,
                           @Param("startTime") OffsetDateTime startTime,
                           @Param("endTime") OffsetDateTime endTime);

    @Query("""
            SELECT s FROM Sheet s
            JOIN FETCH s.lesson l
            JOIN FETCH l.workoutType wt
            JOIN FETCH l.teacher t
            JOIN FETCH l.campus c
            WHERE s.student.id = :studentId
            AND (cast(:from as timestamp) IS NULL OR l.endTime >= :from)
            AND (cast(:to as timestamp) IS NULL OR l.startTime <= :to)
            AND (cast(:visited as boolean) IS NULL OR s.visited = :visited)
            AND (
                cast(:status as string) = 'UPCOMING' AND l.startTime > :now
                OR cast(:status as string) = 'ONGOING' AND l.startTime <= :now AND l.endTime >= :now
                OR cast(:status as string) = 'PAST' AND l.endTime < :now
                OR cast(:status as string) IS NULL AND l.endTime >= :now
            )
            """)
    Page<Sheet> findAllMyLessons(
            @Param("studentId") UUID studentId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("visited") Boolean visited,
            @Param("status") String status,
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );
}
