package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hse.sportclassbookingbackend.dto.teacher.TeacherPatchRequest;
import ru.hse.sportclassbookingbackend.dto.teacher.TeacherResponse;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.TeacherMapper;
import ru.hse.sportclassbookingbackend.model.Teacher;
import ru.hse.sportclassbookingbackend.repository.TeacherRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService{
    private final TeacherRepository teacherRepository;
    private final TeacherMapper teacherMapper;

    public void delete(UUID id) {
        teacherRepository.findById(id).ifPresent(teacher -> {
            teacher.setIsActive(false);
            teacherRepository.save(teacher);
        });
    }

    @Override
    public TeacherResponse update(UUID id, TeacherPatchRequest request) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Teacher with id: " + id + " was not found"));


        teacherMapper.toTeacherFromDto(request, teacher);

        if (request.position() != null) {
            teacher.setPosition(request.position());
        }

        return teacherMapper.toResponse(teacherRepository.save(teacher));
    }

    public TeacherResponse getById(UUID id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Teacher with id: " + id + " was not found"));
        return teacherMapper.toResponse(teacher);
    }

    public List<TeacherResponse> getAll() {
        return teacherRepository.findAllByIsActiveTrue().stream().map(teacherMapper::toResponse).toList();
    }
}
