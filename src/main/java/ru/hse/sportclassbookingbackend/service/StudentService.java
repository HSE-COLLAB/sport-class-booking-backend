package ru.hse.sportclassbookingbackend.service;

import ru.hse.sportclassbookingbackend.dto.student.StudentHealthGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.student.StudentResponse;
import ru.hse.sportclassbookingbackend.dto.user.UserPatchRequest;
import ru.hse.sportclassbookingbackend.dto.user.UserResponse;

import java.util.List;
import java.util.UUID;

public interface StudentService {
    void delete(UUID id);

    UserResponse update(UUID id, UserPatchRequest request);

    StudentResponse updateHealthGroup(UUID id, StudentHealthGroupPatchRequest request);

    UserResponse getById(UUID id);

    List<UserResponse> getAll();

    List<StudentResponse> getByGroupId(UUID id);
}
