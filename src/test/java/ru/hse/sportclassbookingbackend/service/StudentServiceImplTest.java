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
import ru.hse.sportclassbookingbackend.dto.student.StudentHealthGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.student.StudentPatchRequest;
import ru.hse.sportclassbookingbackend.dto.student.StudentResponse;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    private static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_STUDENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID GROUP_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Integer HEALTH_GROUP_ID = 2;
    private static final Integer OTHER_HEALTH_GROUP_ID = 3;

    private static final String CURRENT_EMAIL = "current@hse.ru";
    private static final String NEW_EMAIL = "new@hse.ru";
    private static final String NEW_PASSWORD = "newPassword123";
    private static final String ENCODED_PASSWORD = "encoded_password";

    @Mock private StudentRepository studentRepository;
    @Mock private StudentMapper studentMapper;
    @Mock private UserRepository userRepository;
    @Mock private StudentGroupRepository studentGroupRepository;
    @Mock private HealthGroupRepository healthGroupRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private StudentServiceImpl studentService;

    private Student student;
    private StudentResponse studentResponse;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(STUDENT_ID);
        student.setEmail(CURRENT_EMAIL);
        student.setFirstName("Ivan");
        student.setLastName("Petrov");
        student.setIsActive(true);

        studentResponse = new StudentResponse(
                STUDENT_ID, GROUP_ID, HEALTH_GROUP_ID, CURRENT_EMAIL,
                "Ivan", "Petrov", null, "STUDENT", true
        );
    }

    // ───── delete ─────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("Делает soft delete активного студента-успехTest")
        void softDeletesActiveStudentSuccessTest() {
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

            studentService.delete(STUDENT_ID);

            assertThat(student.getIsActive()).isFalse();
            verify(studentRepository).save(student);
        }

        @Test
        @DisplayName("Ничего не делает если студент не найден-успехTest")
        void doesNothingWhenStudentMissingTest() {
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

            studentService.delete(STUDENT_ID);

            verify(studentRepository, never()).save(any());
        }
    }

    // ───── update ─────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Обновляет студента-успехTest")
        void updatesStudentSuccessTest() {
            StudentPatchRequest request = new StudentPatchRequest(
                    null, null, "NewName", null, null, null, null, null, null
            );
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(studentRepository.save(student)).thenReturn(student);
            when(studentMapper.toResponse(student)).thenReturn(studentResponse);

            StudentResponse result = studentService.update(STUDENT_ID, request);

            assertThat(result).isEqualTo(studentResponse);
            verify(studentMapper).toStudentFromDto(request, student);
        }

        @Test
        @DisplayName("Перехеширует пароль если он передан-успехTest")
        void reHashesPasswordWhenProvidedTest() {
            StudentPatchRequest request = new StudentPatchRequest(
                    null, NEW_PASSWORD, null, null, null, null, null, null, null
            );
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(studentRepository.save(student)).thenReturn(student);
            when(studentMapper.toResponse(student)).thenReturn(studentResponse);

            studentService.update(STUDENT_ID, request);

            assertThat(student.getPassword()).isEqualTo(ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("Обновляет студенческую группу если передан groupId-успехTest")
        void updatesGroupWhenGroupIdProvidedTest() {
            StudentGroup newGroup = new StudentGroup();
            newGroup.setId(GROUP_ID);

            StudentPatchRequest request = new StudentPatchRequest(
                    null, null, null, null, null, null, GROUP_ID, null, null
            );
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(studentGroupRepository.getReferenceById(GROUP_ID)).thenReturn(newGroup);
            when(studentRepository.save(student)).thenReturn(student);
            when(studentMapper.toResponse(student)).thenReturn(studentResponse);

            studentService.update(STUDENT_ID, request);

            assertThat(student.getGroup()).isEqualTo(newGroup);
        }

        @Test
        @DisplayName("Обновляет медгруппу если передан healthGroupId-успехTest")
        void updatesHealthGroupWhenHealthGroupIdProvidedTest() {
            HealthGroup newHealthGroup = new HealthGroup();
            newHealthGroup.setId(HEALTH_GROUP_ID);

            StudentPatchRequest request = new StudentPatchRequest(
                    null, null, null, null, null, null, null, HEALTH_GROUP_ID, null
            );
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(healthGroupRepository.getReferenceById(HEALTH_GROUP_ID)).thenReturn(newHealthGroup);
            when(studentRepository.save(student)).thenReturn(student);
            when(studentMapper.toResponse(student)).thenReturn(studentResponse);

            studentService.update(STUDENT_ID, request);

            assertThat(student.getHealthGroup()).isEqualTo(newHealthGroup);
        }

        @Test
        @DisplayName("Бросает NotFoundException если студент не найден-ошибкаTest")
        void throwsNotFoundWhenStudentMissingTest() {
            StudentPatchRequest request = new StudentPatchRequest(
                    null, null, "Name", null, null, null, null, null, null
            );
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.update(STUDENT_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(STUDENT_ID.toString());
        }

        @Test
        @DisplayName("Бросает ConflictException если email уже занят-ошибкаTest")
        void throwsConflictWhenEmailAlreadyTakenTest() {
            StudentPatchRequest request = new StudentPatchRequest(
                    NEW_EMAIL, null, null, null, null, null, null, null, null
            );
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> studentService.update(STUDENT_ID, request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining(NEW_EMAIL);
        }

        @Test
        @DisplayName("Не проверяет email если он совпадает с текущим-успехTest")
        void doesNotCheckEmailWhenSameAsCurrentTest() {
            StudentPatchRequest request = new StudentPatchRequest(
                    CURRENT_EMAIL, null, null, null, null, null, null, null, null
            );
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(studentRepository.save(student)).thenReturn(student);
            when(studentMapper.toResponse(student)).thenReturn(studentResponse);

            studentService.update(STUDENT_ID, request);

            verify(userRepository, never()).existsByEmail(any());
        }
    }

    // ───── updateHealthGroup ─────

    @Nested
    @DisplayName("updateHealthGroup")
    class UpdateHealthGroup {

        @Test
        @DisplayName("Меняет медгруппу студента-успехTest")
        void changesStudentHealthGroupSuccessTest() {
            HealthGroup newHealthGroup = new HealthGroup();
            newHealthGroup.setId(OTHER_HEALTH_GROUP_ID);

            StudentHealthGroupPatchRequest request = new StudentHealthGroupPatchRequest(OTHER_HEALTH_GROUP_ID);
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(healthGroupRepository.findById(OTHER_HEALTH_GROUP_ID)).thenReturn(Optional.of(newHealthGroup));
            when(studentRepository.save(student)).thenReturn(student);
            when(studentMapper.toResponse(student)).thenReturn(studentResponse);

            studentService.updateHealthGroup(STUDENT_ID, request);

            assertThat(student.getHealthGroup()).isEqualTo(newHealthGroup);
        }

        @Test
        @DisplayName("Бросает NotFoundException если студент не найден-ошибкаTest")
        void throwsNotFoundWhenStudentMissingTest() {
            StudentHealthGroupPatchRequest request = new StudentHealthGroupPatchRequest(HEALTH_GROUP_ID);
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.updateHealthGroup(STUDENT_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Student");
        }

        @Test
        @DisplayName("Бросает NotFoundException если медгруппа не найдена-ошибкаTest")
        void throwsNotFoundWhenHealthGroupMissingTest() {
            StudentHealthGroupPatchRequest request = new StudentHealthGroupPatchRequest(HEALTH_GROUP_ID);
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(healthGroupRepository.findById(HEALTH_GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.updateHealthGroup(STUDENT_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("HealthGroup");
        }
    }

    // ───── getById ─────

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("Возвращает студента по id-успехTest")
        void returnsStudentByIdSuccessTest() {
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(studentMapper.toResponse(student)).thenReturn(studentResponse);

            StudentResponse result = studentService.getById(STUDENT_ID);

            assertThat(result).isEqualTo(studentResponse);
        }

        @Test
        @DisplayName("Бросает NotFoundException если студент не найден-ошибкаTest")
        void throwsNotFoundWhenStudentMissingTest() {
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.getById(STUDENT_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(STUDENT_ID.toString());
        }
    }

    // ───── getAll ─────

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("Возвращает только активных студентов-успехTest")
        void returnsOnlyActiveStudentsSuccessTest() {
            Student other = new Student();
            other.setId(OTHER_STUDENT_ID);

            StudentResponse otherResponse = new StudentResponse(
                    OTHER_STUDENT_ID, null, null, null, null, null, null, "STUDENT", true
            );

            when(studentRepository.findAllByIsActiveTrue()).thenReturn(List.of(student, other));
            when(studentMapper.toResponse(student)).thenReturn(studentResponse);
            when(studentMapper.toResponse(other)).thenReturn(otherResponse);

            List<StudentResponse> result = studentService.getAll();

            assertThat(result).containsExactly(studentResponse, otherResponse);
        }

        @Test
        @DisplayName("Возвращает пустой список если активных нет-успехTest")
        void returnsEmptyListWhenNoActiveStudentsTest() {
            when(studentRepository.findAllByIsActiveTrue()).thenReturn(List.of());

            List<StudentResponse> result = studentService.getAll();

            assertThat(result).isEmpty();
        }
    }

    // ───── selfUpdate ─────

    @Nested
    @DisplayName("selfUpdate")
    class SelfUpdate {

        @Test
        @DisplayName("Студент обновляет свой профиль-успехTest")
        void studentUpdatesOwnProfileSuccessTest() {
            StudentSelfUpdateRequest request = new StudentSelfUpdateRequest(
                    null, null, "NewFirstName", null, null
            );
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(studentRepository.save(student)).thenReturn(student);
            when(studentMapper.toResponse(student)).thenReturn(studentResponse);

            StudentResponse result = studentService.selfUpdate(STUDENT_ID, request);

            assertThat(result).isEqualTo(studentResponse);
            verify(studentMapper).toStudentFromSelfUpdate(request, student);
        }

        @Test
        @DisplayName("Перехеширует пароль при self-update если он передан-успехTest")
        void reHashesPasswordOnSelfUpdateWhenProvidedTest() {
            StudentSelfUpdateRequest request = new StudentSelfUpdateRequest(
                    null, NEW_PASSWORD, null, null, null
            );
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(studentRepository.save(student)).thenReturn(student);
            when(studentMapper.toResponse(student)).thenReturn(studentResponse);

            studentService.selfUpdate(STUDENT_ID, request);

            assertThat(student.getPassword()).isEqualTo(ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("Бросает NotFoundException если студент не найден-ошибкаTest")
        void throwsNotFoundWhenStudentMissingTest() {
            StudentSelfUpdateRequest request = new StudentSelfUpdateRequest(
                    null, null, "Name", null, null
            );
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.selfUpdate(STUDENT_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(STUDENT_ID.toString());
        }

        @Test
        @DisplayName("Бросает ConflictException если email уже занят-ошибкаTest")
        void throwsConflictWhenEmailAlreadyTakenTest() {
            StudentSelfUpdateRequest request = new StudentSelfUpdateRequest(
                    NEW_EMAIL, null, null, null, null
            );
            when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> studentService.selfUpdate(STUDENT_ID, request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining(NEW_EMAIL);
        }
    }
}