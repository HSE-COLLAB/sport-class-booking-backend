package ru.hse.sportclassbookingbackend.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "workout_types")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class WorkoutType {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allow_health_group_id")
    private HealthGroup allowHealthGroup;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @PrePersist
    private void generateId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
