package ru.hse.sportclassbookingbackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teachers")
@DiscriminatorValue("TEACHER")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Teacher extends User {

    @Column(name = "position")
    private String position;
}
