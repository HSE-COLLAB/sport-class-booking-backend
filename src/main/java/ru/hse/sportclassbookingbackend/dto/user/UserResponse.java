package ru.hse.sportclassbookingbackend.dto.user;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String password,
        String first_name,
        String last_name,
        String middle_name,
        String role,
        Boolean is_active
) {
}