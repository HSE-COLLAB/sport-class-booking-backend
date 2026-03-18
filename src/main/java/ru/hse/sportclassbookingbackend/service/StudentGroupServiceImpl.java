package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hse.sportclassbookingbackend.dto.student_group.StudentGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.student_group.StudentGroupRequest;
import ru.hse.sportclassbookingbackend.dto.student_group.StudentGroupResponse;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.StudentGroupMapper;
import ru.hse.sportclassbookingbackend.model.StudentGroup;
import ru.hse.sportclassbookingbackend.repository.StudentGroupRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentGroupServiceImpl implements StudentGroupService {
    private final StudentGroupRepository studentGroupRepository;
    private final StudentGroupMapper studentGroupMapper;

    @Override
    public List<StudentGroupResponse> getAll() {
        List<StudentGroup> studentGroups = studentGroupRepository.findAll();
        List<StudentGroupResponse> studentGroupResponses = new ArrayList<>();
        for (StudentGroup studentGroup : studentGroups) {
            studentGroupResponses.add(studentGroupMapper.toResponse(studentGroup));
        }
        return studentGroupResponses;
    }

    @Override
    public StudentGroupResponse create(StudentGroupRequest request) {
        StudentGroup studentGroup = studentGroupMapper.toEntity(request);
        studentGroupRepository.save(studentGroup);
        return studentGroupMapper.toResponse(studentGroup);
    }

    @Override
    public StudentGroupResponse patch(UUID id, StudentGroupPatchRequest request) {
        StudentGroup studentGroup = studentGroupRepository.findById(id).orElseThrow(() -> new NotFoundException("Student group with id: " + id + " not found"));
        studentGroupMapper.updateFromPatch(request, studentGroup);
        studentGroupRepository.save(studentGroup);
        return studentGroupMapper.toResponse(studentGroup);
    }

    @Override
    public void delete(UUID id) {
        studentGroupRepository.deleteById(id);
    }
}
