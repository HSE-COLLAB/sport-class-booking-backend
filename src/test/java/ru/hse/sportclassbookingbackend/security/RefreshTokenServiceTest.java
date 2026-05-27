package ru.hse.sportclassbookingbackend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import ru.hse.sportclassbookingbackend.model.Role;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final String REFRESH_PREFIX = "refresh:";
    private static final Long REFRESH_EXPIRATION_MILLIS = 2_629_746_000L;
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID REFRESH_TOKEN = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private RedisOperations<Object, Object> redisOperations;
    @Mock private HashOperations<Object, Object, Object> hashOperations;
    @Mock private HashOperations<String, Object, Object> templateHashOperations;

    @InjectMocks private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpiration", REFRESH_EXPIRATION_MILLIS);
    }

    // ───── create ─────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Создаёт refresh-токен и сохраняет userId+role в Redis-успехTest")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void createsRefreshTokenAndStoresInRedisSuccessTest() {
            when(redisOperations.opsForHash()).thenReturn(hashOperations);
            when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(inv -> {
                SessionCallback<Object> callback = inv.getArgument(0);
                return callback.execute(redisOperations);
            });

            UUID result = refreshTokenService.create(USER_ID, Role.STUDENT);

            assertThat(result).isNotNull();

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Map<String, String>> fieldsCaptor = ArgumentCaptor.forClass(Map.class);

            verify(redisOperations).multi();
            verify(hashOperations).putAll(keyCaptor.capture(), fieldsCaptor.capture());
            verify(redisOperations).expire(eq(keyCaptor.getValue()), eq(REFRESH_EXPIRATION_MILLIS), eq(TimeUnit.MILLISECONDS));
            verify(redisOperations).exec();

            assertThat(keyCaptor.getValue()).isEqualTo(REFRESH_PREFIX + result);
            assertThat(fieldsCaptor.getValue())
                    .containsEntry("userId", USER_ID.toString())
                    .containsEntry("role", Role.STUDENT.name());
        }

        @Test
        @DisplayName("Откатывает транзакцию через discard при ошибке-ошибкаTest")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void discardsTransactionOnErrorTest() {
            RuntimeException cause = new RuntimeException("redis broke");
            when(redisOperations.opsForHash()).thenReturn(hashOperations);
            doThrowOnPutAll(cause);
            when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(inv -> {
                SessionCallback<Object> callback = inv.getArgument(0);
                return callback.execute(redisOperations);
            });

            assertThatThrownBy(() -> refreshTokenService.create(USER_ID, Role.STUDENT))
                    .isSameAs(cause);

            verify(redisOperations).multi();
            verify(redisOperations).discard();
            verify(redisOperations, never()).exec();
        }

        @SuppressWarnings("unchecked")
        private void doThrowOnPutAll(RuntimeException ex) {
            org.mockito.Mockito.doThrow(ex).when(hashOperations).putAll(anyString(), anyMap());
        }
    }

    // ───── getTokenData ─────

    @Nested
    @DisplayName("getTokenData")
    class GetTokenData {

        @Test
        @DisplayName("Возвращает данные токена если ключ найден-успехTest")
        void returnsTokenDataWhenKeyFoundTest() {
            Map<Object, Object> entries = new HashMap<>();
            entries.put("userId", USER_ID.toString());
            entries.put("role", Role.TEACHER.name());

            when(redisTemplate.opsForHash()).thenReturn(templateHashOperations);
            when(templateHashOperations.entries(REFRESH_PREFIX + REFRESH_TOKEN)).thenReturn(entries);

            Optional<RefreshTokenService.RefreshTokenData> result = refreshTokenService.getTokenData(REFRESH_TOKEN);

            assertThat(result).isPresent();
            assertThat(result.get().userId()).isEqualTo(USER_ID);
            assertThat(result.get().role()).isEqualTo(Role.TEACHER);
        }

        @Test
        @DisplayName("Возвращает Optional.empty если ключа нет-успехTest")
        void returnsEmptyWhenKeyMissingTest() {
            when(redisTemplate.opsForHash()).thenReturn(templateHashOperations);
            when(templateHashOperations.entries(REFRESH_PREFIX + REFRESH_TOKEN)).thenReturn(new HashMap<>());

            Optional<RefreshTokenService.RefreshTokenData> result = refreshTokenService.getTokenData(REFRESH_TOKEN);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Возвращает Optional.empty если в хеше нет userId-ошибкаTest")
        void returnsEmptyWhenUserIdMissingInHashTest() {
            Map<Object, Object> entries = new HashMap<>();
            entries.put("role", Role.ADMIN.name());
            entries.put("someOtherField", "value");

            when(redisTemplate.opsForHash()).thenReturn(templateHashOperations);
            when(templateHashOperations.entries(REFRESH_PREFIX + REFRESH_TOKEN)).thenReturn(entries);

            Optional<RefreshTokenService.RefreshTokenData> result = refreshTokenService.getTokenData(REFRESH_TOKEN);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Возвращает Optional.empty если в хеше нет role-ошибкаTest")
        void returnsEmptyWhenRoleMissingInHashTest() {
            Map<Object, Object> entries = new HashMap<>();
            entries.put("userId", USER_ID.toString());
            entries.put("someOtherField", "value");

            when(redisTemplate.opsForHash()).thenReturn(templateHashOperations);
            when(templateHashOperations.entries(REFRESH_PREFIX + REFRESH_TOKEN)).thenReturn(entries);

            Optional<RefreshTokenService.RefreshTokenData> result = refreshTokenService.getTokenData(REFRESH_TOKEN);

            assertThat(result).isEmpty();
        }
    }

    // ───── delete ─────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("Удаляет ключ из Redis по refresh-токену-успехTest")
        void deletesKeyByRefreshTokenSuccessTest() {
            refreshTokenService.delete(REFRESH_TOKEN);

            verify(redisTemplate).delete(REFRESH_PREFIX + REFRESH_TOKEN);
        }
    }
}