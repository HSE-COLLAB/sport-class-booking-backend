package ru.hse.sportclassbookingbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hse.sportclassbookingbackend.event.UserRegisteredEvent;
import ru.hse.sportclassbookingbackend.dto.auth.AuthResponse;
import ru.hse.sportclassbookingbackend.dto.auth.LoginRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterResponse;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterStudentRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterTeacherRequest;
import ru.hse.sportclassbookingbackend.exception.BadRequestException;
import ru.hse.sportclassbookingbackend.exception.ConflictException;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.exception.UnauthorizedException;
import ru.hse.sportclassbookingbackend.mapper.StudentMapper;
import ru.hse.sportclassbookingbackend.mapper.TeacherMapper;
import ru.hse.sportclassbookingbackend.model.Campus;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.model.Role;
import ru.hse.sportclassbookingbackend.model.Student;
import ru.hse.sportclassbookingbackend.model.StudentGroup;
import ru.hse.sportclassbookingbackend.model.Teacher;
import ru.hse.sportclassbookingbackend.model.User;
import ru.hse.sportclassbookingbackend.repository.CampusRepository;
import ru.hse.sportclassbookingbackend.repository.HealthGroupRepository;
import ru.hse.sportclassbookingbackend.repository.StudentGroupRepository;
import ru.hse.sportclassbookingbackend.repository.UserRepository;
import ru.hse.sportclassbookingbackend.security.JwtService;
import ru.hse.sportclassbookingbackend.security.RefreshTokenService;
import ru.hse.sportclassbookingbackend.service.mail.EmailService;
import ru.hse.sportclassbookingbackend.service.mail.EmailVerificationTokenService;
import ru.hse.sportclassbookingbackend.service.mail.PasswordResetCodeService;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final StudentGroupRepository studentGroupRepository;

    private final HealthGroupRepository healthGroupRepository;

    private final CampusRepository campusRepository;

    private final UserRepository userRepository;

    private final JwtService jwtService;

    private final RefreshTokenService refreshTokenService;

    private final StudentMapper studentMapper;

    private final TeacherMapper teacherMapper;

    private final PasswordEncoder passwordEncoder;

    private final EmailVerificationTokenService emailVerificationTokenService;

    private final PasswordResetCodeService passwordResetCodeService;

    private final EmailService emailService;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public RegisterResponse registerStudent(RegisterStudentRequest request) {
        Student student = studentMapper.toEntity(request);
        prepareEmailSlot(student.getEmail());
        StudentGroup sg = studentGroupRepository.findById(request.groupId())
                .orElseThrow(() -> new NotFoundException("Student group with id " + request.groupId() + " not found"));
        student.setGroup(sg);
        student.setRole(Role.STUDENT);
        student.setHealthGroup(healthGroupRepository.getReferenceById(HealthGroup.DEFAULT_HEALTH_GROUP_ID));
        student.setCampus(findCampusOrThrow(request.campusId()));
        return commonRegisterStage(student);
    }

    @Override
    @Transactional
    public RegisterResponse registerTeacher(RegisterTeacherRequest request) {
        Teacher teacher = teacherMapper.toEntity(request);
        prepareEmailSlot(teacher.getEmail());
        teacher.setRole(Role.TEACHER);
        teacher.setCampus(findCampusOrThrow(request.campusId()));
        return commonRegisterStage(teacher);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword()))
            throw new UnauthorizedException("Invalid email or password");

        if (Boolean.FALSE.equals(user.getEmailVerified()))
            throw new UnauthorizedException("Email is not verified. Check your inbox or request a new verification email.");

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

    @Override
    @Transactional
    public void verifyEmail(UUID token) {
        UUID userId = emailVerificationTokenService.consume(token)
                .orElseThrow(() -> new BadRequestException("Verification token is invalid, expired, or already used"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id " + userId + " not found"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }

        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Override
    public void resendVerification(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("Resend-verification requested for unknown email: {}", email);
            return;
        }
        User user = userOpt.get();
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            log.info("Resend-verification requested for already-verified email: {}", email);
            return;
        }
        UUID token = emailVerificationTokenService.create(user.getId());
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), token);
    }

    @Override
    public void forgotPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("Forgot-password requested for unknown email: {}", email);
            return;
        }
        User user = userOpt.get();
        String code = passwordResetCodeService.create(email);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), code);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        if (!passwordResetCodeService.verifyAndConsume(email, code)) {
            throw new BadRequestException("Reset code is invalid, expired, or attempt limit exceeded");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Reset code is invalid, expired, or attempt limit exceeded"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenService.deleteAllForUser(user.getId());
        log.info("Password reset successful for user {}", user.getId());
    }

    private void prepareEmailSlot(String email) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isEmpty()) {
            return;
        }
        User user = existing.get();
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ConflictException("User with email " + email + " already exists");
        }
        log.info("Replacing unverified user {} with new registration on same email", user.getId());
        userRepository.delete(user);
        userRepository.flush();
    }

    private Campus findCampusOrThrow(Integer campusId) {
        return campusRepository.findById(campusId)
                .orElseThrow(() -> new NotFoundException("Campus with id " + campusId + " not found"));
    }

    private RegisterResponse commonRegisterStage(User user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user = userRepository.save(user);
        UUID token = emailVerificationTokenService.create(user.getId());
        eventPublisher.publishEvent(new UserRegisteredEvent(
                user.getId(), user.getEmail(), user.getFirstName(), token
        ));
        return RegisterResponse.of(user.getEmail());
    }
}
