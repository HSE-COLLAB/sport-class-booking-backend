package ru.hse.sportclassbookingbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.hse.sportclassbookingbackend.dto.auth.AuthResponse;
import ru.hse.sportclassbookingbackend.dto.auth.LoginRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterStudentRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterTeacherRequest;
import ru.hse.sportclassbookingbackend.exception.BadRequestException;
import ru.hse.sportclassbookingbackend.exception.ConflictException;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GROUP_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID REFRESH_TOKEN = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID UNKNOWN_REFRESH_TOKEN = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final Integer CAMPUS_ID = 3;
    private static final String EMAIL = "user@hse.ru";
    private static final String RAW_PASSWORD = "rawPassword";
    private static final String WRONG_PASSWORD = "wrongPassword";
    private static final String ENCODED_PASSWORD = "encoded_password";
    private static final String ACCESS_TOKEN = "access.jwt.token";
    private static final String FIRST_NAME = "Ivan";
    private static final String LAST_NAME = "Petrov";
    private static final String MIDDLE_NAME = "Sergeevich";
    private static final String POSITION = "Доцент";

    @Mock private StudentGroupRepository studentGroupRepository;
    @Mock private HealthGroupRepository healthGroupRepository;
    @Mock private CampusRepository campusRepository;
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private StudentMapper studentMapper;
    @Mock private TeacherMapper teacherMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthServiceImpl authService;

    private Campus campus;
    private StudentGroup studentGroup;
    private HealthGroup defaultHealthGroup;

    @BeforeEach
    void setUp() {
        campus = new Campus();
        campus.setId(CAMPUS_ID);

        studentGroup = new StudentGroup();
        studentGroup.setId(GROUP_ID);

        defaultHealthGroup = new HealthGroup();
        defaultHealthGroup.setId(HealthGroup.DEFAULT_HEALTH_GROUP_ID);
    }

    // ───── registerStudent ─────

    @Nested
    @DisplayName("registerStudent")
    class RegisterStudent {

        @Test
        @DisplayName("Регистрирует студента и возвращает токены-успехTest")
        void registersStudentAndReturnsTokensSuccessTest() {
            RegisterStudentRequest request = studentRequest();
            Student studentFromMapper = newStudent();

            when(studentMapper.toEntity(request)).thenReturn(studentFromMapper);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(studentGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(studentGroup));
            when(healthGroupRepository.getReferenceById(HealthGroup.DEFAULT_HEALTH_GROUP_ID))
                    .thenReturn(defaultHealthGroup);
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(studentFromMapper)).thenAnswer(inv -> {
                Student s = inv.getArgument(0);
                s.setId(USER_ID);
                return s;
            });
            when(refreshTokenService.create(USER_ID, Role.STUDENT)).thenReturn(REFRESH_TOKEN);
            when(jwtService.generateAccessToken(USER_ID, Role.STUDENT)).thenReturn(ACCESS_TOKEN);

            AuthResponse result = authService.registerStudent(request);

            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(studentFromMapper.getRole()).isEqualTo(Role.STUDENT);
            assertThat(studentFromMapper.getGroup()).isEqualTo(studentGroup);
            assertThat(studentFromMapper.getCampus()).isEqualTo(campus);
            assertThat(studentFromMapper.getHealthGroup()).isEqualTo(defaultHealthGroup);
            assertThat(studentFromMapper.getPassword()).isEqualTo(ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("Бросает ConflictException если email уже занят-ошибкаTest")
        void throwsConflictWhenEmailAlreadyExistsTest() {
            RegisterStudentRequest request = studentRequest();
            Student studentFromMapper = newStudent();

            when(studentMapper.toEntity(request)).thenReturn(studentFromMapper);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> authService.registerStudent(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining(EMAIL);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Бросает BadRequestException если студенческая группа не найдена-ошибкаTest")
        void throwsBadRequestWhenStudentGroupMissingTest() {
            RegisterStudentRequest request = studentRequest();
            Student studentFromMapper = newStudent();

            when(studentMapper.toEntity(request)).thenReturn(studentFromMapper);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(studentGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registerStudent(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Student group");
        }

        @Test
        @DisplayName("Бросает BadRequestException если кампус не найден-ошибкаTest")
        void throwsBadRequestWhenCampusMissingTest() {
            RegisterStudentRequest request = studentRequest();
            Student studentFromMapper = newStudent();

            when(studentMapper.toEntity(request)).thenReturn(studentFromMapper);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(studentGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(studentGroup));
            when(healthGroupRepository.getReferenceById(HealthGroup.DEFAULT_HEALTH_GROUP_ID))
                    .thenReturn(defaultHealthGroup);
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registerStudent(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Campus");
        }
    }

    // ───── registerTeacher ─────

    @Nested
    @DisplayName("registerTeacher")
    class RegisterTeacher {

        @Test
        @DisplayName("Регистрирует препода и возвращает токены-успехTest")
        void registersTeacherAndReturnsTokensSuccessTest() {
            RegisterTeacherRequest request = teacherRequest();
            Teacher teacherFromMapper = newTeacher();

            when(teacherMapper.toEntity(request)).thenReturn(teacherFromMapper);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(teacherFromMapper)).thenAnswer(inv -> {
                Teacher t = inv.getArgument(0);
                t.setId(USER_ID);
                return t;
            });
            when(refreshTokenService.create(USER_ID, Role.TEACHER)).thenReturn(REFRESH_TOKEN);
            when(jwtService.generateAccessToken(USER_ID, Role.TEACHER)).thenReturn(ACCESS_TOKEN);

            AuthResponse result = authService.registerTeacher(request);

            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(teacherFromMapper.getRole()).isEqualTo(Role.TEACHER);
            assertThat(teacherFromMapper.getCampus()).isEqualTo(campus);
            assertThat(teacherFromMapper.getPassword()).isEqualTo(ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("Бросает ConflictException если email уже занят-ошибкаTest")
        void throwsConflictWhenEmailAlreadyExistsTest() {
            RegisterTeacherRequest request = teacherRequest();
            Teacher teacherFromMapper = newTeacher();

            when(teacherMapper.toEntity(request)).thenReturn(teacherFromMapper);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> authService.registerTeacher(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining(EMAIL);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Бросает BadRequestException если кампус не найден-ошибкаTest")
        void throwsBadRequestWhenCampusMissingTest() {
            RegisterTeacherRequest request = teacherRequest();
            Teacher teacherFromMapper = newTeacher();

            when(teacherMapper.toEntity(request)).thenReturn(teacherFromMapper);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registerTeacher(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Campus");
        }
    }

    // ───── login ─────

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("Возвращает токены при корректных кредах-успехTest")
        void returnsTokensOnValidCredentialsSuccessTest() {
            LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
            User user = existingUser();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(refreshTokenService.create(USER_ID, Role.STUDENT)).thenReturn(REFRESH_TOKEN);
            when(jwtService.generateAccessToken(USER_ID, Role.STUDENT)).thenReturn(ACCESS_TOKEN);

            AuthResponse result = authService.login(request);

            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Бросает UnauthorizedException если юзера с таким email нет-ошибкаTest")
        void throwsUnauthorizedWhenUserMissingTest() {
            LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining(EMAIL);

            verify(jwtService, never()).generateAccessToken(any(), any());
            verify(refreshTokenService, never()).create(any(), any());
        }

        @Test
        @DisplayName("Бросает UnauthorizedException при неверном пароле-ошибкаTest")
        void throwsUnauthorizedOnInvalidPasswordTest() {
            LoginRequest request = new LoginRequest(EMAIL, WRONG_PASSWORD);
            User user = existingUser();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(WRONG_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid password");

            verify(jwtService, never()).generateAccessToken(any(), any());
            verify(refreshTokenService, never()).create(any(), any());
        }
    }

    // ───── refresh ─────

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("Возвращает новый access-токен с тем же refresh-токеном-успехTest")
        void returnsNewAccessWithSameRefreshSuccessTest() {
            RefreshTokenService.RefreshTokenData data =
                    new RefreshTokenService.RefreshTokenData(USER_ID, Role.STUDENT);

            when(refreshTokenService.getTokenData(REFRESH_TOKEN)).thenReturn(Optional.of(data));
            when(jwtService.generateAccessToken(USER_ID, Role.STUDENT)).thenReturn(ACCESS_TOKEN);

            AuthResponse result = authService.refresh(REFRESH_TOKEN);

            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Бросает UnauthorizedException если refresh-токен неизвестен-ошибкаTest")
        void throwsUnauthorizedWhenRefreshTokenUnknownTest() {
            when(refreshTokenService.getTokenData(UNKNOWN_REFRESH_TOKEN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(UNKNOWN_REFRESH_TOKEN))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid token");

            verify(jwtService, never()).generateAccessToken(any(), any());
        }
    }

    // ───── logout ─────

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("Удаляет refresh-токен из хранилища-успехTest")
        void deletesRefreshTokenSuccessTest() {
            authService.logout(REFRESH_TOKEN);

            verify(refreshTokenService).delete(REFRESH_TOKEN);
        }
    }

    // ───── helpers ─────

    private RegisterStudentRequest studentRequest() {
        return new RegisterStudentRequest(
                FIRST_NAME, LAST_NAME, MIDDLE_NAME, EMAIL, RAW_PASSWORD, GROUP_ID, CAMPUS_ID
        );
    }

    private RegisterTeacherRequest teacherRequest() {
        return new RegisterTeacherRequest(
                FIRST_NAME, LAST_NAME, MIDDLE_NAME, EMAIL, RAW_PASSWORD, POSITION, CAMPUS_ID
        );
    }

    private Student newStudent() {
        Student s = new Student();
        s.setEmail(EMAIL);
        s.setPassword(RAW_PASSWORD);
        s.setFirstName(FIRST_NAME);
        s.setLastName(LAST_NAME);
        s.setMiddleName(MIDDLE_NAME);
        return s;
    }

    private Teacher newTeacher() {
        Teacher t = new Teacher();
        t.setEmail(EMAIL);
        t.setPassword(RAW_PASSWORD);
        t.setFirstName(FIRST_NAME);
        t.setLastName(LAST_NAME);
        t.setMiddleName(MIDDLE_NAME);
        t.setPosition(POSITION);
        return t;
    }

    private User existingUser() {
        Student u = new Student();
        u.setId(USER_ID);
        u.setEmail(EMAIL);
        u.setPassword(ENCODED_PASSWORD);
        u.setRole(Role.STUDENT);
        return u;
    }
}