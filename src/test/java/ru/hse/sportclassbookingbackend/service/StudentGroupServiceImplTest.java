package ru.hse.sportclassbookingbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hse.sportclassbookingbackend.dto.studentgroup.StudentGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.studentgroup.StudentGroupRequest;
import ru.hse.sportclassbookingbackend.dto.studentgroup.StudentGroupResponse;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.StudentGroupMapper;
import ru.hse.sportclassbookingbackend.model.StudentGroup;
import ru.hse.sportclassbookingbackend.repository.StudentGroupRepository;

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
class StudentGroupServiceImplTest {

    private static final UUID GROUP_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_GROUP_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID UNKNOWN_GROUP_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private static final String FACULTY = "ФКН";
    private static final String ACADEMIC_MAJOR = "Программная инженерия";
    private static final String GROUP_NUMBER = "ПИ-2024";

    @Mock private StudentGroupRepository studentGroupRepository;
    @Mock private StudentGroupMapper studentGroupMapper;

    @InjectMocks private StudentGroupServiceImpl studentGroupService;

    private StudentGroup studentGroup;
    private StudentGroupResponse studentGroupResponse;

    @BeforeEach
    void setUp() {
        studentGroup = new StudentGroup();
        studentGroup.setId(GROUP_ID);
        studentGroup.setFaculty(FACULTY);
        studentGroup.setAcademicMajor(ACADEMIC_MAJOR);
        studentGroup.setGroupNumber(GROUP_NUMBER);

        studentGroupResponse = new StudentGroupResponse(GROUP_ID, FACULTY, ACADEMIC_MAJOR, GROUP_NUMBER);
    }

    // ───── getAll ─────

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("Возвращает все студенческие группы-успехTest")
        void returnsAllStudentGroupsSuccessTest() {
            StudentGroup other = new StudentGroup();
            other.setId(OTHER_GROUP_ID);
            StudentGroupResponse otherResponse =
                    new StudentGroupResponse(OTHER_GROUP_ID, FACULTY, ACADEMIC_MAJOR, "ПИ-2023");

            when(studentGroupRepository.findAll()).thenReturn(List.of(studentGroup, other));
            when(studentGroupMapper.toResponse(studentGroup)).thenReturn(studentGroupResponse);
            when(studentGroupMapper.toResponse(other)).thenReturn(otherResponse);

            List<StudentGroupResponse> result = studentGroupService.getAll();

            assertThat(result).containsExactly(studentGroupResponse, otherResponse);
        }

        @Test
        @DisplayName("Возвращает пустой список если групп нет-успехTest")
        void returnsEmptyListWhenNoGroupsTest() {
            when(studentGroupRepository.findAll()).thenReturn(List.of());

            List<StudentGroupResponse> result = studentGroupService.getAll();

            assertThat(result).isEmpty();
        }
    }

    // ───── create ─────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Создаёт студенческую группу-успехTest")
        void createsStudentGroupSuccessTest() {
            StudentGroupRequest request = new StudentGroupRequest(FACULTY, ACADEMIC_MAJOR, GROUP_NUMBER);
            StudentGroup fromMapper = new StudentGroup();

            when(studentGroupMapper.toEntity(request)).thenReturn(fromMapper);
            when(studentGroupRepository.save(fromMapper)).thenReturn(studentGroup);
            when(studentGroupMapper.toResponse(studentGroup)).thenReturn(studentGroupResponse);

            StudentGroupResponse result = studentGroupService.create(request);

            assertThat(result).isEqualTo(studentGroupResponse);
            verify(studentGroupRepository).save(fromMapper);
        }
    }

    // ───── update ─────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Обновляет студенческую группу через маппер-успехTest")
        void updatesStudentGroupViaMapperSuccessTest() {
            StudentGroupPatchRequest request = new StudentGroupPatchRequest(
                    "МИЭМ", null, null
            );
            when(studentGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(studentGroup));
            when(studentGroupMapper.toResponse(studentGroup)).thenReturn(studentGroupResponse);

            StudentGroupResponse result = studentGroupService.update(GROUP_ID, request);

            assertThat(result).isEqualTo(studentGroupResponse);
            verify(studentGroupMapper).updateFromPatch(request, studentGroup);
            verify(studentGroupRepository).save(studentGroup);
        }

        @Test
        @DisplayName("Бросает NotFoundException если группа не найдена-ошибкаTest")
        void throwsNotFoundWhenGroupMissingTest() {
            StudentGroupPatchRequest request = new StudentGroupPatchRequest("МИЭМ", null, null);
            when(studentGroupRepository.findById(UNKNOWN_GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> studentGroupService.update(UNKNOWN_GROUP_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(UNKNOWN_GROUP_ID.toString());

            verify(studentGroupRepository, never()).save(any());
        }
    }

    // ───── delete ─────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("Удаляет студенческую группу по id-успехTest")
        void deletesStudentGroupByIdSuccessTest() {
            studentGroupService.delete(GROUP_ID);

            verify(studentGroupRepository).deleteById(GROUP_ID);
        }
    }
}