package ru.hse.sportclassbookingbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.hse.sportclassbookingbackend.model.HealthGroup;

public interface HealthGroupRepository extends JpaRepository<HealthGroup, Integer> {}
