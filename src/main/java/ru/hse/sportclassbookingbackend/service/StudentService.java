package ru.hse.sportclassbookingbackend.service;

import ru.hse.sportclassbookingbackend.dto.student.StudentHealthGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.student.StudentResponse;
import ru.hse.sportclassbookingbackend.dto.student.StudentPatchRequest;
import ru.hse.sportclassbookingbackend.dto.student.StudentSelfUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface StudentService {
    void delete(UUID id);

    StudentResponse update(UUID id, StudentPatchRequest request);

    StudentResponse updateHealthGroup(UUID id, StudentHealthGroupPatchRequest request);

    StudentResponse getById(UUID id);

    List<StudentResponse> getAll();

    StudentResponse selfUpdate(UUID id, StudentSelfUpdateRequest request);
}
