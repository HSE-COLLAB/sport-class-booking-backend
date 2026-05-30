package ru.hse.sportclassbookingbackend.service.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final long TTL_MS = 86_400_000L;

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks private EmailVerificationTokenService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "verificationExpiration", TTL_MS);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("create записывает в Redis под ключом email-verify:{token} с TTL-успехTest")
    void createStoresTokenWithTtlTest() {
        UUID token = service.create(USER_ID);

        assertThat(token).isNotNull();
        verify(valueOperations).set(
                eq("email-verify:" + token),
                eq(USER_ID.toString()),
                eq(TTL_MS),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("consume возвращает userId и удаляет ключ (атомарный getAndDelete)-успехTest")
    void consumeReturnsUserIdAndDeletesTest() {
        UUID token = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(valueOperations.getAndDelete("email-verify:" + token)).thenReturn(USER_ID.toString());

        Optional<UUID> result = service.consume(token);

        assertThat(result).isPresent().contains(USER_ID);
    }

    @Test
    @DisplayName("consume на неизвестный/протухший токен возвращает Optional.empty-ошибкаTest")
    void consumeReturnsEmptyForUnknownTokenTest() {
        UUID token = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        when(valueOperations.getAndDelete("email-verify:" + token)).thenReturn(null);

        Optional<UUID> result = service.consume(token);

        assertThat(result).isEmpty();
    }
}
