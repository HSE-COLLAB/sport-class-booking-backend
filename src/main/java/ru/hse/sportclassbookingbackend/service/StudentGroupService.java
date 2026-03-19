package ru.hse.sportclassbookingbackend.service;

import ru.hse.sportclassbookingbackend.dto.studentgroup.StudentGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.studentgroup.StudentGroupRequest;
import ru.hse.sportclassbookingbackend.dto.studentgroup.StudentGroupResponse;

import java.util.List;
import java.util.UUID;

public interface StudentGroupService {

    List<StudentGroupResponse> getAll();

    StudentGroupResponse create(StudentGroupRequest request);

    StudentGroupResponse patch(UUID id, StudentGroupPatchRequest request);

    void delete(UUID id);
}
