package ru.hse.sportclassbookingbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.hse.sportclassbookingbackend.model.Campus;

public interface CampusRepository extends JpaRepository<Campus, Integer> {
}
