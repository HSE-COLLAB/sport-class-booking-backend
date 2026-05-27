package ru.hse.sportclassbookingbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupRequest;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupResponse;
import ru.hse.sportclassbookingbackend.exception.NotFoundException;
import ru.hse.sportclassbookingbackend.mapper.HealthGroupMapper;
import ru.hse.sportclassbookingbackend.model.HealthGroup;
import ru.hse.sportclassbookingbackend.repository.HealthGroupRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthGroupServiceImplTest {

    private static final int HEALTH_GROUP_ID = 1;
    private static final int OTHER_HEALTH_GROUP_ID = 2;
    private static final int UNKNOWN_HEALTH_GROUP_ID = 999;

    private static final String DESCRIPTION = "Основная группа";
    private static final String UPDATED_DESCRIPTION = "Подготовительная группа";

    @Mock private HealthGroupRepository healthGroupRepository;
    @Mock private HealthGroupMapper healthGroupMapper;

    @InjectMocks private HealthGroupServiceImpl healthGroupService;

    private HealthGroup healthGroup;
    private HealthGroupResponse healthGroupResponse;

    @BeforeEach
    void setUp() {
        healthGroup = new HealthGroup();
        healthGroup.setId(HEALTH_GROUP_ID);
        healthGroup.setDescription(DESCRIPTION);

        healthGroupResponse = new HealthGroupResponse(HEALTH_GROUP_ID, DESCRIPTION);
    }

    // ───── getAll ─────

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("Возвращает все медгруппы-успехTest")
        void returnsAllHealthGroupsSuccessTest() {
            HealthGroup other = new HealthGroup();
            other.setId(OTHER_HEALTH_GROUP_ID);
            HealthGroupResponse otherResponse =
                    new HealthGroupResponse(OTHER_HEALTH_GROUP_ID, "Подготовительная группа");

            when(healthGroupRepository.findAll()).thenReturn(List.of(healthGroup, other));
            when(healthGroupMapper.toResponse(healthGroup)).thenReturn(healthGroupResponse);
            when(healthGroupMapper.toResponse(other)).thenReturn(otherResponse);

            List<HealthGroupResponse> result = healthGroupService.getAll();

            assertThat(result).containsExactly(healthGroupResponse, otherResponse);
        }

        @Test
        @DisplayName("Возвращает пустой список если медгрупп нет-успехTest")
        void returnsEmptyListWhenNoHealthGroupsTest() {
            when(healthGroupRepository.findAll()).thenReturn(List.of());

            List<HealthGroupResponse> result = healthGroupService.getAll();

            assertThat(result).isEmpty();
        }
    }

    // ───── getById ─────

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("Возвращает медгруппу по id-успехTest")
        void returnsHealthGroupByIdSuccessTest() {
            when(healthGroupRepository.findById(HEALTH_GROUP_ID)).thenReturn(Optional.of(healthGroup));
            when(healthGroupMapper.toResponse(healthGroup)).thenReturn(healthGroupResponse);

            HealthGroupResponse result = healthGroupService.getById(HEALTH_GROUP_ID);

            assertThat(result).isEqualTo(healthGroupResponse);
        }

        @Test
        @DisplayName("Бросает NotFoundException если медгруппа не найдена-ошибкаTest")
        void throwsNotFoundWhenHealthGroupMissingTest() {
            when(healthGroupRepository.findById(UNKNOWN_HEALTH_GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> healthGroupService.getById(UNKNOWN_HEALTH_GROUP_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(String.valueOf(UNKNOWN_HEALTH_GROUP_ID));
        }
    }

    // ───── update ─────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Обновляет медгруппу через маппер-успехTest")
        void updatesHealthGroupViaMapperSuccessTest() {
            HealthGroupRequest request = new HealthGroupRequest(UPDATED_DESCRIPTION);
            when(healthGroupRepository.findById(HEALTH_GROUP_ID)).thenReturn(Optional.of(healthGroup));
            when(healthGroupRepository.save(healthGroup)).thenReturn(healthGroup);
            when(healthGroupMapper.toResponse(healthGroup)).thenReturn(healthGroupResponse);

            HealthGroupResponse result = healthGroupService.update(HEALTH_GROUP_ID, request);

            assertThat(result).isEqualTo(healthGroupResponse);
            verify(healthGroupMapper).updateFromPut(request, healthGroup);
            verify(healthGroupRepository).save(healthGroup);
        }

        @Test
        @DisplayName("Бросает NotFoundException если медгруппа не найдена-ошибкаTest")
        void throwsNotFoundWhenHealthGroupMissingTest() {
            HealthGroupRequest request = new HealthGroupRequest(UPDATED_DESCRIPTION);
            when(healthGroupRepository.findById(UNKNOWN_HEALTH_GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> healthGroupService.update(UNKNOWN_HEALTH_GROUP_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(String.valueOf(UNKNOWN_HEALTH_GROUP_ID));

            verify(healthGroupRepository, never()).save(any());
        }
    }
}