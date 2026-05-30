package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hse.sportclassbookingbackend.event.HealthGroupChangedEvent;
import ru.hse.sportclassbookingbackend.dto.student.StudentHealthGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.student.StudentResponse;
import ru.hse.sportclassbookingbackend.dto.student.StudentPatchRequest;
import ru.hse.sportclassbookingbackend.dto.student.StudentSelfUpdateRequest;
import ru.hse.sportclassbookingbackend.exception.ConflictException;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.StudentMapper;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.model.Student;
import ru.hse.sportclassbookingbackend.model.StudentGroup;
import ru.hse.sportclassbookingbackend.repository.HealthGroupRepository;
import ru.hse.sportclassbookingbackend.repository.StudentGroupRepository;
import ru.hse.sportclassbookingbackend.repository.StudentRepository;
import ru.hse.sportclassbookingbackend.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService{
    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final UserRepository userRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final HealthGroupRepository healthGroupRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void delete(UUID id) {
        studentRepository.findById(id).ifPresent(student -> {
            student.setIsActive(false);
            studentRepository.save(student);
            log.info("Student deactivated: studentId={}", id);
        });
    }

    @Override
    public StudentResponse update(UUID id, StudentPatchRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student with id: " + id + " was not found"));

        checkEmailAvailable(request.email(), student.getEmail());

        studentMapper.toStudentFromDto(request, student);

        if (request.password() != null) {
            student.setPassword(passwordEncoder.encode(request.password()));
        }

        if (request.groupId() != null) {
            StudentGroup studentGroup = studentGroupRepository.getReferenceById(request.groupId());
            student.setGroup(studentGroup);
        }

        if (request.healthGroupId() != null) {
            HealthGroup healthGroup = healthGroupRepository.getReferenceById(request.healthGroupId());
            student.setHealthGroup(healthGroup);
        }

        Student saved = studentRepository.save(student);
        log.info("Student updated: studentId={}", saved.getId());
        return studentMapper.toResponse(saved);
    }

    @Transactional
    public StudentResponse updateHealthGroup(UUID id, StudentHealthGroupPatchRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student with id: " + id + " was not found"));

        HealthGroup healthGroup = healthGroupRepository.findById(request.healthGroupId())
                .orElseThrow(() -> new NotFoundException("HealthGroup with id " + request.healthGroupId() + " not found"));

        Integer oldGroupId = student.getHealthGroup() != null ? student.getHealthGroup().getId() : null;
        student.setHealthGroup(healthGroup);
        Student saved = studentRepository.save(student);

        if (oldGroupId == null || !oldGroupId.equals(healthGroup.getId())) {
            eventPublisher.publishEvent(new HealthGroupChangedEvent(saved.getId(), oldGroupId, healthGroup.getId()));
            log.info("Student health group changed: studentId={} oldGroupId={} newGroupId={} (notification email queued)",
                    saved.getId(), oldGroupId, healthGroup.getId());
        } else {
            log.info("Student health group unchanged: studentId={} groupId={}", saved.getId(), healthGroup.getId());
        }

        return studentMapper.toResponse(saved);
    }

    public StudentResponse getById(UUID id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student with id: " + id + " was not found"));
        return studentMapper.toResponse(student);
    }

    public List<StudentResponse> getAll() {
        return studentRepository.findAllByIsActiveTrue().stream().map(studentMapper::toResponse).toList();
    }

    public StudentResponse selfUpdate(UUID id, StudentSelfUpdateRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student with id: " + id + " was not found"));

        checkEmailAvailable(request.email(), student.getEmail());

        studentMapper.toStudentFromSelfUpdate(request, student);

        if (request.password() != null) {
            student.setPassword(passwordEncoder.encode(request.password()));
        }

        Student saved = studentRepository.save(student);
        log.info("Student self-updated: studentId={}", saved.getId());
        return studentMapper.toResponse(saved);
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
