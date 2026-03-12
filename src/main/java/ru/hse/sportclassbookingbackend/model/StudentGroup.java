package ru.hse.sportclassbookingbackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class StudentGroup {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "faculty", nullable = false)
    private String faculty;

    @Column(name = "academic_major", nullable = false)
    private String academicMajor;

    @Column(name = "group_number", nullable = false)
    private String groupNumber;

    @PrePersist
    private void generateId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
