package ru.hse.sportclassbookingbackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "workout_types")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class WorkoutType {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "workout_type_health_groups",
            joinColumns = @JoinColumn(name = "workout_type_id"),
            inverseJoinColumns = @JoinColumn(name = "health_group_id")
    )
    private Set<HealthGroup> allowedHealthGroups = new HashSet<>();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @PrePersist
    private void generateId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
