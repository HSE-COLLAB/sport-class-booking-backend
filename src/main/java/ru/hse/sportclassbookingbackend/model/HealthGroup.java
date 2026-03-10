package ru.hse.sportclassbookingbackend.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "health_groups")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class HealthGroup {
    @Id
    private int id;

    @Column(nullable = false)
    private String description;
}
