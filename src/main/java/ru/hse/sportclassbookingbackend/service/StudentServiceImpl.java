package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hse.sportclassbookingbackend.dto.student.StudentHealthGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.student.StudentResponse;
import ru.hse.sportclassbookingbackend.dto.user.UserPatchRequest;
import ru.hse.sportclassbookingbackend.dto.user.UserResponse;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.StudentMapper;
import ru.hse.sportclassbookingbackend.mapper.UserMapper;
import ru.hse.sportclassbookingbackend.model.Role;
import ru.hse.sportclassbookingbackend.model.Student;
import ru.hse.sportclassbookingbackend.model.User;
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
    private final UserMapper userMapper;

    @Override
    public void delete(UUID id) {
        userRepository.findByIdAndIsActiveTrue(id).ifPresent(s -> {
            s.setIsActive(false);
            userRepository.save(s);
        });
    }

    @Override
    public UserResponse update(UUID id, UserPatchRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Workout type with id: " + id + " was not found"));

        if (request.email() != null) {
            user.setEmail(request.email());
        }

        if (request.password() != null) {
            user.setPassword(request.password());
        }

        if (request.first_name() != null) {
            user.setFirstName(request.first_name());
        }

        if (request.last_name() != null) {
            user.setLastName(request.last_name());
        }

        if (request.middle_name() != null) {
            user.setMiddleName(request.middle_name());
        }

        if (request.role() != null) {
            user.setRole(Role.valueOf(request.role()));
        }
        return userMapper.toResponse(userRepository.save(user));
    }

    public StudentResponse updateHealthGroup(UUID id, StudentHealthGroupPatchRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student with id: " + id + " was not found"));
        student.setHealthGroup(request.health_group_id());
        return studentMapper.toResponse(studentRepository.save(student));
    }

    public UserResponse getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Workout type with id: " + id + " was not found"));
        return userMapper.toResponse(user);
    }

    public List<UserResponse> getAll() {
        return userRepository.findAllByIsActiveTrueAndRole(Role.STUDENT).stream().map(userMapper::toResponse).toList();
    }

    public List<StudentResponse> getByGroupId(UUID id) {
        return studentRepository.findAllByIsActiveTrueAndGroupId(id).stream().map(studentMapper::toResponse).toList();
    }


}
