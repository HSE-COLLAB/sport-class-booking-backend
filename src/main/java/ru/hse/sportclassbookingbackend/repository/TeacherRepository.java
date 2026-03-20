package ru.hse.sportclassbookingbackend.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.hse.sportclassbookingbackend.model.Teacher;

import java.util.UUID;

public interface TeacherRepository extends JpaRepository<Teacher, UUID> {

}
