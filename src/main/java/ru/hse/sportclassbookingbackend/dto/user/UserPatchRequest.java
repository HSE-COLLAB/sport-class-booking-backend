package ru.hse.sportclassbookingbackend.dto.user;

public record UserPatchRequest(
        String email,
        String password,
        String first_name,
        String last_name,
        String middle_name,
        String role,
        Boolean is_active
) {
}
