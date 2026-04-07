package ru.hse.sportclassbookingbackend.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "students")
@DiscriminatorValue("STUDENT")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Student extends User {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private StudentGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_group_id")
    private HealthGroup healthGroup;

    @OneToMany(mappedBy = "student", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Sheet> sheets = new ArrayList<>();
}
