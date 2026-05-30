package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.hse.sportclassbookingbackend.dto.studentgroup.StudentGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.studentgroup.StudentGroupRequest;
import ru.hse.sportclassbookingbackend.dto.studentgroup.StudentGroupResponse;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.StudentGroupMapper;
import ru.hse.sportclassbookingbackend.model.StudentGroup;
import ru.hse.sportclassbookingbackend.repository.StudentGroupRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentGroupServiceImpl implements StudentGroupService {

    private final StudentGroupRepository studentGroupRepository;

    private final StudentGroupMapper studentGroupMapper;

    @Override
    public List<StudentGroupResponse> getAll() {
        return studentGroupRepository.findAll()
                .stream()
                .map(studentGroupMapper::toResponse)
                .toList();
    }

    @Override
    public StudentGroupResponse create(StudentGroupRequest request) {
        StudentGroup saved = studentGroupRepository.save(studentGroupMapper.toEntity(request));
        log.info("StudentGroup created: studentGroupId={}", saved.getId());
        return studentGroupMapper.toResponse(saved);
    }

    @Override
    public StudentGroupResponse update(UUID id, StudentGroupPatchRequest request) {
        StudentGroup studentGroup = studentGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student group with id: " + id + " was not found"));

        studentGroupMapper.updateFromPatch(request, studentGroup);
        studentGroupRepository.save(studentGroup);

        log.info("StudentGroup updated: studentGroupId={}", id);
        return studentGroupMapper.toResponse(studentGroup);
    }

    @Override
    public void delete(UUID id) {
        studentGroupRepository.deleteById(id);
        log.info("StudentGroup deleted: studentGroupId={}", id);
    }
}
