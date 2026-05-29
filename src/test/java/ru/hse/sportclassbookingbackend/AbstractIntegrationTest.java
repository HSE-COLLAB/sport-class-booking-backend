package ru.hse.sportclassbookingbackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import ru.hse.sportclassbookingbackend.dto.auth.AuthResponse;
import ru.hse.sportclassbookingbackend.dto.auth.LoginRequest;
import ru.hse.sportclassbookingbackend.model.Campus;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.model.Role;
import ru.hse.sportclassbookingbackend.model.Student;
import ru.hse.sportclassbookingbackend.model.StudentGroup;
import ru.hse.sportclassbookingbackend.model.Teacher;
import ru.hse.sportclassbookingbackend.model.User;
import ru.hse.sportclassbookingbackend.repository.CampusRepository;
import ru.hse.sportclassbookingbackend.repository.HealthGroupRepository;
import ru.hse.sportclassbookingbackend.repository.LessonRepository;
import ru.hse.sportclassbookingbackend.repository.SheetRepository;
import ru.hse.sportclassbookingbackend.repository.StudentGroupRepository;
import ru.hse.sportclassbookingbackend.repository.UserRepository;
import ru.hse.sportclassbookingbackend.repository.WorkoutTypeRepository;
import ru.hse.sportclassbookingbackend.service.mail.EmailVerificationTokenService;
import ru.hse.sportclassbookingbackend.service.mail.PasswordResetCodeService;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final Integer NN_CAMPUS_ID = 3;
    protected static final Integer DEFAULT_HEALTH_GROUP_ID = 1;
    protected static final String DEFAULT_PASSWORD = "P@ssw0rd123";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"));

    @ServiceConnection(name = "redis")
    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:8.4"));

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected PasswordEncoder passwordEncoder;

    @MockBean(reset = MockReset.NONE) protected JavaMailSender javaMailSender;

    @Autowired protected SheetRepository sheetRepository;
    @Autowired protected LessonRepository lessonRepository;
    @Autowired protected WorkoutTypeRepository workoutTypeRepository;
    @Autowired protected UserRepository userRepository;
    @Autowired protected StudentGroupRepository studentGroupRepository;
    @Autowired protected CampusRepository campusRepository;
    @Autowired protected HealthGroupRepository healthGroupRepository;
    @Autowired protected EmailVerificationTokenService emailVerificationTokenService;
    @Autowired protected PasswordResetCodeService passwordResetCodeService;
    @Autowired protected StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanBusinessData() {
        sheetRepository.deleteAll();
        lessonRepository.deleteAll();
        workoutTypeRepository.deleteAll();
        userRepository.deleteAll();
        studentGroupRepository.deleteAll();
    }

    @BeforeEach
    void setupMailSenderMock() {
        org.mockito.Mockito.when(javaMailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage((Session) null));
    }

    protected Student createStudent(String email, Campus campus, HealthGroup healthGroup, StudentGroup group) {
        Student student = new Student();
        student.setEmail(email);
        student.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        student.setFirstName("Test");
        student.setLastName("Student");
        student.setMiddleName("Middle");
        student.setIsActive(true);
        student.setEmailVerified(true);
        student.setRole(Role.STUDENT);
        student.setCampus(campus);
        student.setHealthGroup(healthGroup);
        student.setGroup(group);
        return userRepository.save(student);
    }

    protected Teacher createTeacher(String email, Campus campus) {
        Teacher teacher = new Teacher();
        teacher.setEmail(email);
        teacher.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        teacher.setFirstName("Test");
        teacher.setLastName("Teacher");
        teacher.setMiddleName("Middle");
        teacher.setIsActive(true);
        teacher.setEmailVerified(true);
        teacher.setRole(Role.TEACHER);
        teacher.setPosition("Преподаватель");
        teacher.setCampus(campus);
        return userRepository.save(teacher);
    }

    protected User createAdmin(String email) {
        User admin = new User();
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        admin.setFirstName("Test");
        admin.setLastName("Admin");
        admin.setMiddleName("Middle");
        admin.setIsActive(true);
        admin.setEmailVerified(true);
        admin.setRole(Role.ADMIN);
        return userRepository.save(admin);
    }

    protected StudentGroup createDefaultStudentGroup() {
        StudentGroup group = new StudentGroup();
        group.setFaculty("ФКН");
        group.setAcademicMajor("Программная инженерия");
        group.setGroupNumber("ПИ-2024");
        return studentGroupRepository.save(group);
    }

    protected String loginAndGetToken(String email) throws Exception {
        LoginRequest request = new LoginRequest(email, DEFAULT_PASSWORD);
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, AuthResponse.class).accessToken();
    }

    protected String bearerHeader(String token) {
        return "Bearer " + token;
    }

    protected void verifyEmailFor(String email) throws Exception {
        UUID userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User with email " + email + " not found"))
                .getId();
        UUID token = emailVerificationTokenService.create(userId);
        mockMvc.perform(get("/auth/verify-email").param("token", token.toString()))
                .andExpect(status().isNoContent());
    }

    protected String readPasswordResetCode(String email) {
        return redisTemplate.opsForValue().get("password-reset:" + email);
    }
}