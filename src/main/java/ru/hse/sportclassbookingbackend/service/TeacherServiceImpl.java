package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.hse.sportclassbookingbackend.dto.teacher.TeacherPatchRequest;
import ru.hse.sportclassbookingbackend.dto.teacher.TeacherResponse;
import ru.hse.sportclassbookingbackend.exception.ConflictException;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.TeacherMapper;
import ru.hse.sportclassbookingbackend.model.Teacher;
import ru.hse.sportclassbookingbackend.repository.TeacherRepository;
import ru.hse.sportclassbookingbackend.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService{
    private final TeacherRepository teacherRepository;
    private final TeacherMapper teacherMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void delete(UUID id) {
        teacherRepository.findById(id).ifPresent(teacher -> {
            teacher.setIsActive(false);
            teacherRepository.save(teacher);
            log.info("Teacher deactivated: teacherId={}", id);
        });
    }

    @Override
    public TeacherResponse update(UUID id, TeacherPatchRequest request) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Teacher with id: " + id + " was not found"));

        checkEmailAvailable(request.email(), teacher.getEmail());

        teacherMapper.toTeacherFromDto(request, teacher);

        if (request.password() != null) {
            teacher.setPassword(passwordEncoder.encode(request.password()));
        }

        if (request.position() != null) {
            teacher.setPosition(request.position());
        }

        Teacher saved = teacherRepository.save(teacher);
        log.info("Teacher updated: teacherId={}", saved.getId());
        return teacherMapper.toResponse(saved);
    }

    public TeacherResponse getById(UUID id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Teacher with id: " + id + " was not found"));
        return teacherMapper.toResponse(teacher);
    }

    public List<TeacherResponse> getAll() {
        return teacherRepository.findAllByIsActiveTrue().stream().map(teacherMapper::toResponse).toList();
    }

    private void checkEmailAvailable(String newEmail, String currentEmail) {
        if (newEmail == null || newEmail.equals(currentEmail)) {
            return;
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("User with email " + newEmail + " already exists");
        }
    }
}
