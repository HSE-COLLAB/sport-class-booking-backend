package ru.hse.sportclassbookingbackend.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Lesson {
    @Id
    private UUID id;

    @Column
    private String title;

    @Column
    private String place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_type_id")
    private WorkoutType workoutType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(name = "total_places", nullable = false)
    private int totalPlaces;

    @Column
    private String notes;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Sheet> sheets = new ArrayList<>();

    @PrePersist
    private void generateId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
