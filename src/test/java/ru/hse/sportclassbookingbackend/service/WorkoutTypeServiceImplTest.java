package ru.hse.sportclassbookingbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypePatchRequest;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeRequest;
import ru.hse.sportclassbookingbackend.dto.workouttype.WorkoutTypeResponse;
import ru.hse.sportclassbookingbackend.exception.BadRequestException;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.WorkoutTypeMapper;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.model.WorkoutType;
import ru.hse.sportclassbookingbackend.repository.HealthGroupRepository;
import ru.hse.sportclassbookingbackend.repository.WorkoutTypeRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutTypeServiceImplTest {

    private static final UUID WORKOUT_TYPE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_WORKOUT_TYPE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final Integer HEALTH_GROUP_ID_1 = 1;
    private static final Integer HEALTH_GROUP_ID_2 = 2;
    private static final Integer UNKNOWN_HEALTH_GROUP_ID = 999;

    private static final String NEW_TITLE = "Аэробика";
    private static final String UPDATED_TITLE = "Силовая";

    @Mock private WorkoutTypeRepository workoutTypeRepository;
    @Mock private HealthGroupRepository healthGroupRepository;
    @Mock private WorkoutTypeMapper workoutTypeMapper;

    @InjectMocks private WorkoutTypeServiceImpl workoutTypeService;

    private WorkoutType workoutType;
    private WorkoutTypeResponse workoutTypeResponse;
    private HealthGroup healthGroup1;
    private HealthGroup healthGroup2;

    @BeforeEach
    void setUp() {
        healthGroup1 = new HealthGroup();
        healthGroup1.setId(HEALTH_GROUP_ID_1);

        healthGroup2 = new HealthGroup();
        healthGroup2.setId(HEALTH_GROUP_ID_2);

        workoutType = new WorkoutType();
        workoutType.setId(WORKOUT_TYPE_ID);
        workoutType.setTitle("Волейбол");
        workoutType.setIsActive(true);

        workoutTypeResponse = new WorkoutTypeResponse(WORKOUT_TYPE_ID, "Волейбол", List.of());
    }

    // ───── getAll ─────

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("Возвращает только активные типы тренировок-успехTest")
        void returnsOnlyActiveWorkoutTypesSuccessTest() {
            WorkoutType other = new WorkoutType();
            other.setId(OTHER_WORKOUT_TYPE_ID);
            WorkoutTypeResponse otherResponse =
                    new WorkoutTypeResponse(OTHER_WORKOUT_TYPE_ID, "Йога", List.of());

            when(workoutTypeRepository.findAllByIsActiveTrue()).thenReturn(List.of(workoutType, other));
            when(workoutTypeMapper.toResponse(workoutType)).thenReturn(workoutTypeResponse);
            when(workoutTypeMapper.toResponse(other)).thenReturn(otherResponse);

            List<WorkoutTypeResponse> result = workoutTypeService.getAll();

            assertThat(result).containsExactly(workoutTypeResponse, otherResponse);
        }

        @Test
        @DisplayName("Возвращает пустой список если активных нет-успехTest")
        void returnsEmptyListWhenNoActiveWorkoutTypesTest() {
            when(workoutTypeRepository.findAllByIsActiveTrue()).thenReturn(List.of());

            List<WorkoutTypeResponse> result = workoutTypeService.getAll();

            assertThat(result).isEmpty();
        }
    }

    // ───── getById ─────

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("Возвращает тип тренировки по id-успехTest")
        void returnsWorkoutTypeByIdSuccessTest() {
            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID))
                    .thenReturn(Optional.of(workoutType));
            when(workoutTypeMapper.toResponse(workoutType)).thenReturn(workoutTypeResponse);

            WorkoutTypeResponse result = workoutTypeService.getById(WORKOUT_TYPE_ID);

            assertThat(result).isEqualTo(workoutTypeResponse);
        }

        @Test
        @DisplayName("Бросает NotFoundException если тип тренировки не найден или неактивен-ошибкаTest")
        void throwsNotFoundWhenWorkoutTypeMissingOrInactiveTest() {
            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> workoutTypeService.getById(WORKOUT_TYPE_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(WORKOUT_TYPE_ID.toString());
        }
    }

    // ───── create ─────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Создаёт тип тренировки и помечает его активным-успехTest")
        void createsWorkoutTypeAndMarksActiveSuccessTest() {
            WorkoutTypeRequest request = new WorkoutTypeRequest(
                    NEW_TITLE, Set.of(HEALTH_GROUP_ID_1, HEALTH_GROUP_ID_2)
            );
            WorkoutType fromMapper = new WorkoutType();
            fromMapper.setTitle(NEW_TITLE);

            when(workoutTypeMapper.toEntity(request)).thenReturn(fromMapper);
            when(healthGroupRepository.findAllById(request.allowedHealthGroupIds()))
                    .thenReturn(List.of(healthGroup1, healthGroup2));
            when(workoutTypeRepository.save(fromMapper)).thenAnswer(inv -> inv.getArgument(0));
            when(workoutTypeMapper.toResponse(fromMapper)).thenReturn(workoutTypeResponse);

            workoutTypeService.create(request);

            assertThat(fromMapper.getIsActive()).isTrue();
            assertThat(fromMapper.getAllowedHealthGroups())
                    .containsExactlyInAnyOrder(healthGroup1, healthGroup2);
            verify(workoutTypeRepository).save(fromMapper);
        }

        @Test
        @DisplayName("Бросает BadRequestException если не все медгруппы найдены-ошибкаTest")
        void throwsBadRequestWhenSomeHealthGroupsMissingTest() {
            WorkoutTypeRequest request = new WorkoutTypeRequest(
                    NEW_TITLE, Set.of(HEALTH_GROUP_ID_1, UNKNOWN_HEALTH_GROUP_ID)
            );
            WorkoutType fromMapper = new WorkoutType();

            when(workoutTypeMapper.toEntity(request)).thenReturn(fromMapper);
            when(healthGroupRepository.findAllById(request.allowedHealthGroupIds()))
                    .thenReturn(List.of(healthGroup1));

            assertThatThrownBy(() -> workoutTypeService.create(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("health group");

            verify(workoutTypeRepository, never()).save(any());
        }
    }

    // ───── update ─────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Обновляет название если оно передано-успехTest")
        void updatesTitleWhenProvidedTest() {
            WorkoutTypePatchRequest request = new WorkoutTypePatchRequest(UPDATED_TITLE, null);

            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID))
                    .thenReturn(Optional.of(workoutType));
            when(workoutTypeRepository.save(workoutType)).thenReturn(workoutType);
            when(workoutTypeMapper.toResponse(workoutType)).thenReturn(workoutTypeResponse);

            workoutTypeService.update(WORKOUT_TYPE_ID, request);

            assertThat(workoutType.getTitle()).isEqualTo(UPDATED_TITLE);
        }

        @Test
        @DisplayName("Обновляет набор медгрупп если он передан-успехTest")
        void updatesHealthGroupsWhenProvidedTest() {
            WorkoutTypePatchRequest request = new WorkoutTypePatchRequest(
                    null, Set.of(HEALTH_GROUP_ID_1, HEALTH_GROUP_ID_2)
            );

            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID))
                    .thenReturn(Optional.of(workoutType));
            when(healthGroupRepository.findAllById(request.allowedHealthGroupIds()))
                    .thenReturn(List.of(healthGroup1, healthGroup2));
            when(workoutTypeRepository.save(workoutType)).thenReturn(workoutType);
            when(workoutTypeMapper.toResponse(workoutType)).thenReturn(workoutTypeResponse);

            workoutTypeService.update(WORKOUT_TYPE_ID, request);

            assertThat(workoutType.getAllowedHealthGroups())
                    .containsExactlyInAnyOrder(healthGroup1, healthGroup2);
        }

        @Test
        @DisplayName("Бросает NotFoundException если тип тренировки не найден-ошибкаTest")
        void throwsNotFoundWhenWorkoutTypeMissingTest() {
            WorkoutTypePatchRequest request = new WorkoutTypePatchRequest(UPDATED_TITLE, null);
            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> workoutTypeService.update(WORKOUT_TYPE_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(WORKOUT_TYPE_ID.toString());
        }

        @Test
        @DisplayName("Бросает BadRequestException если не все новые медгруппы найдены-ошибкаTest")
        void throwsBadRequestWhenSomeNewHealthGroupsMissingTest() {
            WorkoutTypePatchRequest request = new WorkoutTypePatchRequest(
                    null, Set.of(HEALTH_GROUP_ID_1, UNKNOWN_HEALTH_GROUP_ID)
            );

            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID))
                    .thenReturn(Optional.of(workoutType));
            when(healthGroupRepository.findAllById(request.allowedHealthGroupIds()))
                    .thenReturn(List.of(healthGroup1));

            assertThatThrownBy(() -> workoutTypeService.update(WORKOUT_TYPE_ID, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("health group");

            verify(workoutTypeRepository, never()).save(any());
        }
    }

    // ───── delete ─────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("Делает soft delete активного типа-успехTest")
        void softDeletesActiveWorkoutTypeSuccessTest() {
            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID))
                    .thenReturn(Optional.of(workoutType));

            workoutTypeService.delete(WORKOUT_TYPE_ID);

            assertThat(workoutType.getIsActive()).isFalse();
            verify(workoutTypeRepository).save(workoutType);
        }

        @Test
        @DisplayName("Ничего не делает если тип не найден или уже неактивен-успехTest")
        void doesNothingWhenWorkoutTypeMissingOrInactiveTest() {
            when(workoutTypeRepository.findByIdAndIsActiveTrue(WORKOUT_TYPE_ID))
                    .thenReturn(Optional.empty());

            workoutTypeService.delete(WORKOUT_TYPE_ID);

            verify(workoutTypeRepository, never()).save(any());
        }
    }
}