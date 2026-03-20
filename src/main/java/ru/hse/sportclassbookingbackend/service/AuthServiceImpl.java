package ru.hse.sportclassbookingbackend.service;


import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.hse.sportclassbookingbackend.dto.AuthResponse;
import ru.hse.sportclassbookingbackend.dto.LoginRequest;
import ru.hse.sportclassbookingbackend.dto.RegisterStudentRequest;
import ru.hse.sportclassbookingbackend.dto.RegisterTeacherRequest;
import ru.hse.sportclassbookingbackend.exception.ConflictException;
import ru.hse.sportclassbookingbackend.mapper.StudentMapper;
import ru.hse.sportclassbookingbackend.mapper.TeacherMapper;
import ru.hse.sportclassbookingbackend.model.Student;
import ru.hse.sportclassbookingbackend.repository.StudentRepository;
import ru.hse.sportclassbookingbackend.repository.TeacherRepository;
import ru.hse.sportclassbookingbackend.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final StudentRepository studentRepository;

    private final TeacherRepository teacherRepository;

    private final UserRepository userRepository;

    private final StudentMapper studentMapper;

    private final TeacherMapper teacherMapper;

    private final PasswordEncoder passwordEncoder;

    private void checkEmailNotExists(String email){
        if (userRepository.existsByEmail(email))
            throw new ConflictException("User with such email already exists");

    }

    @Override
    public AuthResponse registerStudent(RegisterStudentRequest request) {
        Student student = studentMapper.toEntity(request);
        checkEmailNotExists(student.getEmail());
        // TODO: провалидировать UUID группы студента и присвоить его ему
        student.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(student);

        return null;
    }

    @Override
    public AuthResponse registerTeacher(RegisterTeacherRequest request) {
        return null;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        return null;
    }

    @Override
    public AuthResponse refresh(String refresh) {
        return null;
    }
}
