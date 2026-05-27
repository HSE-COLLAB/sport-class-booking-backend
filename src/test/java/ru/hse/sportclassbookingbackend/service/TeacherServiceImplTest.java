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
import ru.hse.sportclassbookingbackend.dto.teacher.TeacherPatchRequest;
import ru.hse.sportclassbookingbackend.dto.teacher.TeacherResponse;
import ru.hse.sportclassbookingbackend.exception.ConflictException;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.TeacherMapper;
import ru.hse.sportclassbookingbackend.model.Teacher;
import ru.hse.sportclassbookingbackend.repository.TeacherRepository;
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
class TeacherServiceImplTest {

    private static final UUID TEACHER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_TEACHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final String CURRENT_EMAIL = "current@hse.ru";
    private static final String NEW_EMAIL = "new@hse.ru";
    private static final String NEW_PASSWORD = "newPassword123";
    private static final String ENCODED_PASSWORD = "encoded_password";
    private static final String NEW_POSITION = "Старший преподаватель";

    @Mock private TeacherRepository teacherRepository;
    @Mock private TeacherMapper teacherMapper;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private TeacherServiceImpl teacherService;

    private Teacher teacher;
    private TeacherResponse teacherResponse;

    @BeforeEach
    void setUp() {
        teacher = new Teacher();
        teacher.setId(TEACHER_ID);
        teacher.setEmail(CURRENT_EMAIL);
        teacher.setFirstName("Anna");
        teacher.setLastName("Smirnova");
        teacher.setPosition("Доцент");
        teacher.setIsActive(true);

        teacherResponse = new TeacherResponse(
                TEACHER_ID, CURRENT_EMAIL, "Anna", "Smirnova", null,
                "TEACHER", "Доцент", true
        );
    }

    // ───── delete ─────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("Делает soft delete активного препода-успехTest")
        void softDeletesActiveTeacherSuccessTest() {
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));

            teacherService.delete(TEACHER_ID);

            assertThat(teacher.getIsActive()).isFalse();
            verify(teacherRepository).save(teacher);
        }

        @Test
        @DisplayName("Ничего не делает если препод не найден-успехTest")
        void doesNothingWhenTeacherMissingTest() {
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.empty());

            teacherService.delete(TEACHER_ID);

            verify(teacherRepository, never()).save(any());
        }
    }

    // ───── update ─────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Обновляет препода-успехTest")
        void updatesTeacherSuccessTest() {
            TeacherPatchRequest request = new TeacherPatchRequest(
                    null, null, "NewName", null, null, null, null, null
            );
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
            when(teacherRepository.save(teacher)).thenReturn(teacher);
            when(teacherMapper.toResponse(teacher)).thenReturn(teacherResponse);

            TeacherResponse result = teacherService.update(TEACHER_ID, request);

            assertThat(result).isEqualTo(teacherResponse);
            verify(teacherMapper).toTeacherFromDto(request, teacher);
        }

        @Test
        @DisplayName("Перехеширует пароль если он передан-успехTest")
        void reHashesPasswordWhenProvidedTest() {
            TeacherPatchRequest request = new TeacherPatchRequest(
                    null, NEW_PASSWORD, null, null, null, null, null, null
            );
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(teacherRepository.save(teacher)).thenReturn(teacher);
            when(teacherMapper.toResponse(teacher)).thenReturn(teacherResponse);

            teacherService.update(TEACHER_ID, request);

            assertThat(teacher.getPassword()).isEqualTo(ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("Обновляет должность если она передана-успехTest")
        void updatesPositionWhenProvidedTest() {
            TeacherPatchRequest request = new TeacherPatchRequest(
                    null, null, null, null, null, null, null, NEW_POSITION
            );
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
            when(teacherRepository.save(teacher)).thenReturn(teacher);
            when(teacherMapper.toResponse(teacher)).thenReturn(teacherResponse);

            teacherService.update(TEACHER_ID, request);

            assertThat(teacher.getPosition()).isEqualTo(NEW_POSITION);
        }

        @Test
        @DisplayName("Бросает NotFoundException если препод не найден-ошибкаTest")
        void throwsNotFoundWhenTeacherMissingTest() {
            TeacherPatchRequest request = new TeacherPatchRequest(
                    null, null, "Name", null, null, null, null, null
            );
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> teacherService.update(TEACHER_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(TEACHER_ID.toString());
        }

        @Test
        @DisplayName("Бросает ConflictException если email уже занят-ошибкаTest")
        void throwsConflictWhenEmailAlreadyTakenTest() {
            TeacherPatchRequest request = new TeacherPatchRequest(
                    NEW_EMAIL, null, null, null, null, null, null, null
            );
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
            when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> teacherService.update(TEACHER_ID, request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining(NEW_EMAIL);
        }

        @Test
        @DisplayName("Не проверяет email если он совпадает с текущим-успехTest")
        void doesNotCheckEmailWhenSameAsCurrentTest() {
            TeacherPatchRequest request = new TeacherPatchRequest(
                    CURRENT_EMAIL, null, null, null, null, null, null, null
            );
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
            when(teacherRepository.save(teacher)).thenReturn(teacher);
            when(teacherMapper.toResponse(teacher)).thenReturn(teacherResponse);

            teacherService.update(TEACHER_ID, request);

            verify(userRepository, never()).existsByEmail(any());
        }
    }

    // ───── getById ─────

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("Возвращает препода по id-успехTest")
        void returnsTeacherByIdSuccessTest() {
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
            when(teacherMapper.toResponse(teacher)).thenReturn(teacherResponse);

            TeacherResponse result = teacherService.getById(TEACHER_ID);

            assertThat(result).isEqualTo(teacherResponse);
        }

        @Test
        @DisplayName("Бросает NotFoundException если препод не найден-ошибкаTest")
        void throwsNotFoundWhenTeacherMissingTest() {
            when(teacherRepository.findById(TEACHER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> teacherService.getById(TEACHER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(TEACHER_ID.toString());
        }
    }

    // ───── getAll ─────

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("Возвращает только активных преподов-успехTest")
        void returnsOnlyActiveTeachersSuccessTest() {
            Teacher other = new Teacher();
            other.setId(OTHER_TEACHER_ID);

            TeacherResponse otherResponse = new TeacherResponse(
                    OTHER_TEACHER_ID, null, null, null, null, "TEACHER", null, true
            );

            when(teacherRepository.findAllByIsActiveTrue()).thenReturn(List.of(teacher, other));
            when(teacherMapper.toResponse(teacher)).thenReturn(teacherResponse);
            when(teacherMapper.toResponse(other)).thenReturn(otherResponse);

            List<TeacherResponse> result = teacherService.getAll();

            assertThat(result).containsExactly(teacherResponse, otherResponse);
        }

        @Test
        @DisplayName("Возвращает пустой список если активных нет-успехTest")
        void returnsEmptyListWhenNoActiveTeachersTest() {
            when(teacherRepository.findAllByIsActiveTrue()).thenReturn(List.of());

            List<TeacherResponse> result = teacherService.getAll();

            assertThat(result).isEmpty();
        }
    }
}