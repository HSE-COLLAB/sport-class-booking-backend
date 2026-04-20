package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hse.sportclassbookingbackend.dto.student.StudentHealthGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.student.StudentResponse;
import ru.hse.sportclassbookingbackend.dto.student.StudentPatchRequest;
import ru.hse.sportclassbookingbackend.dto.user.UserResponse;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.StudentMapper;
import ru.hse.sportclassbookingbackend.mapper.UserMapper;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.model.Role;
import ru.hse.sportclassbookingbackend.model.Student;
import ru.hse.sportclassbookingbackend.model.StudentGroup;
import ru.hse.sportclassbookingbackend.model.User;
import ru.hse.sportclassbookingbackend.repository.HealthGroupRepository;
import ru.hse.sportclassbookingbackend.repository.StudentGroupRepository;
import ru.hse.sportclassbookingbackend.repository.StudentRepository;
import ru.hse.sportclassbookingbackend.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService{
    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final UserRepository userRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final UserMapper userMapper;
    private final HealthGroupRepository healthGroupRepository;

    @Override
    public void delete(UUID id) {
        userRepository.findByIdAndIsActiveTrue(id).ifPresent(student -> {
            student.setIsActive(false);
            userRepository.save(student);
        });
    }

    @Override
    public StudentResponse update(UUID id, StudentPatchRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student with id: " + id + " was not found"));


        studentMapper.toStudentFromDto(request, student);

        if (request.groupId() != null) {
            StudentGroup studentGroup = studentGroupRepository.getReferenceById(request.groupId());
            student.setGroup(studentGroup);
        }

        if (request.healthGroupId() != null) {
            HealthGroup healthGroup = healthGroupRepository.getReferenceById(request.healthGroupId());
            student.setHealthGroup(healthGroup);
        }

        return studentMapper.toResponse(studentRepository.save(student));
    }

    public StudentResponse updateHealthGroup(UUID id, StudentHealthGroupPatchRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student with id: " + id + " was not found"));

        HealthGroup healthGroup = healthGroupRepository.findById(request.healthGroupId())
                .orElseThrow(() -> new NotFoundException("HealthGroup with id " + request.healthGroupId() + " not found"));

        student.setHealthGroup(healthGroup);
        return studentMapper.toResponse(studentRepository.save(student));
    }

    public StudentResponse getById(UUID id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student with id: " + id + " was not found"));
        return studentMapper.toResponse(student);
    }

    public List<StudentResponse> getAll() {
        return studentRepository.findAllByIsActiveTrue().stream().map(studentMapper::toResponse).toList();
    }



}
