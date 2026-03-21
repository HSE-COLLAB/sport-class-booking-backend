package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hse.sportclassbookingbackend.dto.auth.AuthResponse;
import ru.hse.sportclassbookingbackend.dto.auth.LoginRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterStudentRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterTeacherRequest;
import ru.hse.sportclassbookingbackend.exception.BadRequestException;
import ru.hse.sportclassbookingbackend.exception.ConflictException;
import ru.hse.sportclassbookingbackend.exception.UnauthorizedException;
import ru.hse.sportclassbookingbackend.mapper.StudentMapper;
import ru.hse.sportclassbookingbackend.mapper.TeacherMapper;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.model.Role;
import ru.hse.sportclassbookingbackend.model.Student;
import ru.hse.sportclassbookingbackend.model.StudentGroup;
import ru.hse.sportclassbookingbackend.model.Teacher;
import ru.hse.sportclassbookingbackend.model.User;
import ru.hse.sportclassbookingbackend.repository.HealthGroupRepository;
import ru.hse.sportclassbookingbackend.repository.StudentGroupRepository;
import ru.hse.sportclassbookingbackend.repository.UserRepository;
import ru.hse.sportclassbookingbackend.security.JwtService;
import ru.hse.sportclassbookingbackend.security.RefreshTokenService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final StudentGroupRepository studentGroupRepository;

    private final HealthGroupRepository healthGroupRepository;

    private final UserRepository userRepository;

    private final JwtService jwtService;

    private final RefreshTokenService refreshTokenService;

    private final StudentMapper studentMapper;

    private final TeacherMapper teacherMapper;

    private final PasswordEncoder passwordEncoder;

    private void checkEmailNotExists(String email){
        if (userRepository.existsByEmail(email))
            throw new ConflictException("User with email " + email + " already exists");

    }

    private AuthResponse commonRegisterStage(User user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user = userRepository.save(user);
        UUID refreshToken = refreshTokenService.create(user.getId(), user.getRole());
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        return AuthResponse.of(accessToken, refreshToken);
    }


    @Override
    @Transactional
    public AuthResponse registerStudent(RegisterStudentRequest request) {
        Student student = studentMapper.toEntity(request);
        checkEmailNotExists(student.getEmail());
        StudentGroup sg = studentGroupRepository.findById(request.groupId())
                        .orElseThrow(() -> new BadRequestException("Student group with id " + request.groupId() + " not found"));
        student.setGroup(sg);
        student.setRole(Role.STUDENT);
        student.setHealthGroup(healthGroupRepository.getReferenceById(HealthGroup.DEFAULT_HEALTH_GROUP_ID));
        return commonRegisterStage(student);
    }

    @Override
    @Transactional
    public AuthResponse registerTeacher(RegisterTeacherRequest request) {
        Teacher teacher = teacherMapper.toEntity(request);
        checkEmailNotExists(teacher.getEmail());
        teacher.setRole(Role.TEACHER);
        return commonRegisterStage(teacher);
    }


    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("User with email " + request.email() + " does not exists"));

        if (!passwordEncoder.matches(request.password(), user.getPassword()))
            throw new UnauthorizedException("Invalid password");

        UUID refreshToken = refreshTokenService.create(user.getId(), user.getRole());
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        return AuthResponse.of(accessToken, refreshToken);
    }

    @Override
    public AuthResponse refresh(UUID refresh) {
        RefreshTokenService.RefreshTokenData tokenData = refreshTokenService.getTokenData(refresh)
                .orElseThrow(() -> new UnauthorizedException("Invalid token"));

        String accessToken = jwtService.generateAccessToken(tokenData.userId(), tokenData.role());

        return AuthResponse.of(accessToken, refresh);
    }

    @Override
    public void logout(UUID refresh){
        refreshTokenService.delete(refresh);
    }
}
