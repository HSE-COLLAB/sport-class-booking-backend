package ru.hse.sportclassbookingbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.hse.sportclassbookingbackend.model.WorkoutType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutTypeRepository extends JpaRepository<WorkoutType, UUID> {
    List<WorkoutType> findAllByIsActiveTrue();
    Optional<WorkoutType> findByIdAndIsActiveTrue(UUID id);
}
