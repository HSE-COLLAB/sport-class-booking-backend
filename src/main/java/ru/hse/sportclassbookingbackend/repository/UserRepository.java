package ru.hse.sportclassbookingbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.hse.sportclassbookingbackend.model.Role;
import ru.hse.sportclassbookingbackend.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByIdAndIsActiveTrue(UUID id);
    List<User> findAllByIsActiveTrueAndRole(Role role);
}
