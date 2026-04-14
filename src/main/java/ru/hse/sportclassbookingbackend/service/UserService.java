package ru.hse.sportclassbookingbackend.service;

import ru.hse.sportclassbookingbackend.dto.user.UserPatchRequest;
import ru.hse.sportclassbookingbackend.dto.user.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {
    void delete(UUID id);

    UserResponse update(UUID id, UserPatchRequest request);

    List<UserResponse> getAll();

    UserResponse getById(UUID id);
}
