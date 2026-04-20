package ru.hse.sportclassbookingbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.hse.sportclassbookingbackend.model.Role;
import ru.hse.sportclassbookingbackend.model.Student;
import ru.hse.sportclassbookingbackend.model.User;
import ru.hse.sportclassbookingbackend.model.WorkoutType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    List<Student> findAllByIsActiveTrue();
}
