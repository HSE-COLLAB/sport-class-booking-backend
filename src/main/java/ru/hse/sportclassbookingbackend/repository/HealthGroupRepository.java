package ru.hse.sportclassbookingbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hse.sportclassbookingbackend.model.HealthGroup;

@Repository
public interface HealthGroupRepository extends JpaRepository<HealthGroup, Integer> {}
