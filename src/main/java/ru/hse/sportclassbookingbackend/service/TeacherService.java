package ru.hse.sportclassbookingbackend.service;

import ru.hse.sportclassbookingbackend.dto.teacher.TeacherPatchRequest;
import ru.hse.sportclassbookingbackend.dto.teacher.TeacherResponse;

import java.util.List;
import java.util.UUID;

public interface TeacherService {
    void delete(UUID id);

    TeacherResponse update(UUID id, TeacherPatchRequest request);

    TeacherResponse getById(UUID id);

    List<TeacherResponse> getAll();

}
