package ru.hse.sportclassbookingbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GROUP_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID REFRESH_TOKEN = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID UNKNOWN_REFRESH_TOKEN = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID VERIFY_TOKEN = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID UNKNOWN_VERIFY_TOKEN = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private static final Integer CAMPUS_ID = 3;
    private static final String EMAIL = "user@hse.ru";
    private static final String UNKNOWN_EMAIL = "nobody@hse.ru";
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
    @Mock private EmailVerificationTokenService emailVerificationTokenService;
    @Mock private PasswordResetCodeService passwordResetCodeService;
    @Mock private EmailService emailService;
    @Mock private ApplicationEventPublisher eventPublisher;

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
        @DisplayName("Создаёт unverified-студента, шлёт письмо, возвращает RegisterResponse без токенов-успехTest")
        void createsUnverifiedAndSendsEmailTest() {
            RegisterStudentRequest request = studentRequest();
            Student studentFromMapper = newStudent();

            when(studentMapper.toEntity(request)).thenReturn(studentFromMapper);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
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
            when(emailVerificationTokenService.create(USER_ID)).thenReturn(VERIFY_TOKEN);

            RegisterResponse result = authService.registerStudent(request);

            assertThat(result.email()).isEqualTo(EMAIL);
            assertThat(result.message()).isNotBlank();
            assertThat(studentFromMapper.getRole()).isEqualTo(Role.STUDENT);
            assertThat(studentFromMapper.getGroup()).isEqualTo(studentGroup);
            assertThat(studentFromMapper.getCampus()).isEqualTo(campus);
            assertThat(studentFromMapper.getHealthGroup()).isEqualTo(defaultHealthGroup);
            assertThat(studentFromMapper.getPassword()).isEqualTo(ENCODED_PASSWORD);

            ArgumentCaptor<UserRegisteredEvent> evt = ArgumentCaptor.forClass(UserRegisteredEvent.class);
            verify(eventPublisher).publishEvent(evt.capture());
            assertThat(evt.getValue().email()).isEqualTo(EMAIL);
            assertThat(evt.getValue().firstName()).isEqualTo(FIRST_NAME);
            assertThat(evt.getValue().verificationToken()).isEqualTo(VERIFY_TOKEN);

            verifyNoInteractions(jwtService);
            verify(refreshTokenService, never()).create(any(), any());
        }

        @Test
        @DisplayName("Бросает ConflictException если email занят verified-юзером-ошибкаTest")
        void throwsConflictWhenEmailVerifiedExistsTest() {
            RegisterStudentRequest request = studentRequest();
            Student studentFromMapper = newStudent();
            User existing = existingVerifiedUser();

            when(studentMapper.toEntity(request)).thenReturn(studentFromMapper);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> authService.registerStudent(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining(EMAIL);

            verify(userRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Если email занят unverified-юзером — удаляет старого и продолжает-успехTest")
        void replacesUnverifiedUserOnRepeatedRegistrationTest() {
            RegisterStudentRequest request = studentRequest();
            Student studentFromMapper = newStudent();
            User existingUnverified = existingUnverifiedUser();

            when(studentMapper.toEntity(request)).thenReturn(studentFromMapper);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUnverified));
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
            when(emailVerificationTokenService.create(USER_ID)).thenReturn(VERIFY_TOKEN);

            RegisterResponse result = authService.registerStudent(request);

            assertThat(result.email()).isEqualTo(EMAIL);
            verify(userRepository).delete(existingUnverified);
            verify(userRepository).flush();
            verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
        }

        @Test
        @DisplayName("Бросает NotFoundException если студенческая группа не найдена-ошибкаTest")
        void throwsNotFoundWhenStudentGroupMissingTest() {
            RegisterStudentRequest request = studentRequest();
            Student studentFromMapper = newStudent();

            when(studentMapper.toEntity(request)).thenReturn(studentFromMapper);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(studentGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registerStudent(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Student group");

            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Бросает NotFoundException если кампус не найден-ошибкаTest")
        void throwsNotFoundWhenCampusMissingTest() {
            RegisterStudentRequest request = studentRequest();
            Student studentFromMapper = newStudent();

            when(studentMapper.toEntity(request)).thenReturn(studentFromMapper);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(studentGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(studentGroup));
            when(healthGroupRepository.getReferenceById(HealthGroup.DEFAULT_HEALTH_GROUP_ID))
                    .thenReturn(defaultHealthGroup);
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registerStudent(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Campus");

            verifyNoInteractions(eventPublisher);
        }
    }

    // ───── registerTeacher ─────

    @Nested
    @DisplayName("registerTeacher")
    class RegisterTeacher {

        @Test
        @DisplayName("Создаёт unverified-препода, шлёт письмо, возвращает RegisterResponse-успехTest")
        void createsUnverifiedTeacherTest() {
            RegisterTeacherRequest request = teacherRequest();
            Teacher teacherFromMapper = newTeacher();

            when(teacherMapper.toEntity(request)).thenReturn(teacherFromMapper);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.of(campus));
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(teacherFromMapper)).thenAnswer(inv -> {
                Teacher t = inv.getArgument(0);
                t.setId(USER_ID);
                return t;
            });
            when(emailVerificationTokenService.create(USER_ID)).thenReturn(VERIFY_TOKEN);

            RegisterResponse result = authService.registerTeacher(request);

            assertThat(result.email()).isEqualTo(EMAIL);
            assertThat(teacherFromMapper.getRole()).isEqualTo(Role.TEACHER);
            assertThat(teacherFromMapper.getCampus()).isEqualTo(campus);
            assertThat(teacherFromMapper.getPassword()).isEqualTo(ENCODED_PASSWORD);

            verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Бросает NotFoundException если кампус не найден-ошибкаTest")
        void throwsNotFoundWhenCampusMissingTest() {
            RegisterTeacherRequest request = teacherRequest();
            Teacher teacherFromMapper = newTeacher();

            when(teacherMapper.toEntity(request)).thenReturn(teacherFromMapper);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(campusRepository.findById(CAMPUS_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registerTeacher(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Campus");
        }
    }

    // ───── login ─────

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("Возвращает токены при корректных кредах и verified email-успехTest")
        void returnsTokensOnValidCredentialsTest() {
            LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
            User user = existingVerifiedUser();

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
                    .isInstanceOf(UnauthorizedException.class);

            verify(jwtService, never()).generateAccessToken(any(), any());
            verify(refreshTokenService, never()).create(any(), any());
        }

        @Test
        @DisplayName("Бросает UnauthorizedException при неверном пароле-ошибкаTest")
        void throwsUnauthorizedOnInvalidPasswordTest() {
            LoginRequest request = new LoginRequest(EMAIL, WRONG_PASSWORD);
            User user = existingVerifiedUser();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(WRONG_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class);

            verify(jwtService, never()).generateAccessToken(any(), any());
            verify(refreshTokenService, never()).create(any(), any());
        }

        @Test
        @DisplayName("Бросает UnauthorizedException если email не подтверждён-ошибкаTest")
        void throwsUnauthorizedWhenEmailNotVerifiedTest() {
            LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
            User user = existingUnverifiedUser();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("not verified");

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
        void returnsNewAccessWithSameRefreshTest() {
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
                    .isInstanceOf(UnauthorizedException.class);

            verify(jwtService, never()).generateAccessToken(any(), any());
        }
    }

    // ───── logout ─────

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("Удаляет refresh-токен из хранилища-успехTest")
        void deletesRefreshTokenTest() {
            authService.logout(REFRESH_TOKEN);

            verify(refreshTokenService).delete(REFRESH_TOKEN);
        }
    }

    // ───── verifyEmail ─────

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("По валидному токену ставит emailVerified=true-успехTest")
        void marksUserVerifiedTest() {
            User user = existingUnverifiedUser();
            when(emailVerificationTokenService.consume(VERIFY_TOKEN)).thenReturn(Optional.of(USER_ID));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            authService.verifyEmail(VERIFY_TOKEN);

            assertThat(user.getEmailVerified()).isTrue();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Если юзер уже verified — идемпотентно, save не дёргает-успехTest")
        void idempotentForAlreadyVerifiedUserTest() {
            User user = existingVerifiedUser();
            when(emailVerificationTokenService.consume(VERIFY_TOKEN)).thenReturn(Optional.of(USER_ID));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            authService.verifyEmail(VERIFY_TOKEN);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Бросает BadRequestException на неизвестный/протухший токен-ошибкаTest")
        void throwsBadRequestOnInvalidTokenTest() {
            when(emailVerificationTokenService.consume(UNKNOWN_VERIFY_TOKEN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail(UNKNOWN_VERIFY_TOKEN))
                    .isInstanceOf(BadRequestException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Бросает NotFoundException если userId из токена не существует в БД-ошибкаTest")
        void throwsNotFoundWhenUserMissingTest() {
            when(emailVerificationTokenService.consume(VERIFY_TOKEN)).thenReturn(Optional.of(USER_ID));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail(VERIFY_TOKEN))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ───── resendVerification ─────

    @Nested
    @DisplayName("resendVerification")
    class ResendVerification {

        @Test
        @DisplayName("Для unverified юзера генерит новый токен и шлёт письмо-успехTest")
        void sendsNewEmailForUnverifiedTest() {
            User user = existingUnverifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(emailVerificationTokenService.create(USER_ID)).thenReturn(VERIFY_TOKEN);

            authService.resendVerification(EMAIL);

            verify(emailService).sendVerificationEmail(EMAIL, FIRST_NAME, VERIFY_TOKEN);
        }

        @Test
        @DisplayName("Для unknown email — silent, ничего не шлёт-успехTest")
        void silentForUnknownEmailTest() {
            when(userRepository.findByEmail(UNKNOWN_EMAIL)).thenReturn(Optional.empty());

            authService.resendVerification(UNKNOWN_EMAIL);

            verifyNoInteractions(emailService);
            verify(emailVerificationTokenService, never()).create(any());
        }

        @Test
        @DisplayName("Для уже verified юзера — silent, ничего не шлёт-успехTest")
        void silentForAlreadyVerifiedTest() {
            User user = existingVerifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            authService.resendVerification(EMAIL);

            verifyNoInteractions(emailService);
            verify(emailVerificationTokenService, never()).create(any());
        }
    }

    // ───── forgotPassword ─────

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        @Test
        @DisplayName("Для known email — генерит код и шлёт письмо-успехTest")
        void sendsCodeForKnownEmailTest() {
            User user = existingVerifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordResetCodeService.create(EMAIL)).thenReturn("473829");

            authService.forgotPassword(EMAIL);

            verify(emailService).sendPasswordResetEmail(EMAIL, FIRST_NAME, "473829");
        }

        @Test
        @DisplayName("Для unknown email — silent, ничего не шлёт-успехTest")
        void silentForUnknownEmailTest() {
            when(userRepository.findByEmail(UNKNOWN_EMAIL)).thenReturn(Optional.empty());

            authService.forgotPassword(UNKNOWN_EMAIL);

            verifyNoInteractions(emailService);
            verifyNoInteractions(passwordResetCodeService);
        }
    }

    // ───── resetPassword ─────

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("При валидном коде меняет пароль и отзывает все refresh-токены-успехTest")
        void resetsPasswordAndRevokesSessionsTest() {
            User user = existingVerifiedUser();
            String newPlainPassword = "N3wP@ss";
            String newEncodedPassword = "encoded_new";

            when(passwordResetCodeService.verifyAndConsume(EMAIL, "473829")).thenReturn(true);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(newPlainPassword)).thenReturn(newEncodedPassword);

            authService.resetPassword(EMAIL, "473829", newPlainPassword);

            assertThat(user.getPassword()).isEqualTo(newEncodedPassword);
            verify(userRepository).save(user);
            verify(refreshTokenService).deleteAllForUser(USER_ID);
        }

        @Test
        @DisplayName("Невалидный/истёкший/много-попыток код — BadRequest, ничего не меняется-ошибкаTest")
        void throwsBadRequestOnInvalidCodeTest() {
            when(passwordResetCodeService.verifyAndConsume(EMAIL, "000000")).thenReturn(false);

            assertThatThrownBy(() -> authService.resetPassword(EMAIL, "000000", "any"))
                    .isInstanceOf(BadRequestException.class);

            verify(userRepository, never()).save(any());
            verify(refreshTokenService, never()).deleteAllForUser(any());
        }

        @Test
        @DisplayName("Код валидный, но юзера в БД нет — BadRequest (защита от race)-ошибкаTest")
        void throwsBadRequestWhenUserMissingTest() {
            when(passwordResetCodeService.verifyAndConsume(EMAIL, "473829")).thenReturn(true);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(EMAIL, "473829", "N3w"))
                    .isInstanceOf(BadRequestException.class);

            verify(refreshTokenService, never()).deleteAllForUser(any());
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

    private User existingVerifiedUser() {
        Student u = new Student();
        u.setId(USER_ID);
        u.setEmail(EMAIL);
        u.setPassword(ENCODED_PASSWORD);
        u.setFirstName(FIRST_NAME);
        u.setRole(Role.STUDENT);
        u.setEmailVerified(true);
        return u;
    }

    private User existingUnverifiedUser() {
        Student u = new Student();
        u.setId(USER_ID);
        u.setEmail(EMAIL);
        u.setPassword(ENCODED_PASSWORD);
        u.setFirstName(FIRST_NAME);
        u.setRole(Role.STUDENT);
        u.setEmailVerified(false);
        return u;
    }
}
