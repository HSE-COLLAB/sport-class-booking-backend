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

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetCodeServiceTest {

    private static final String EMAIL = "user@hse.ru";
    private static final long TTL_MS = 600_000L;
    private static final int MAX_ATTEMPTS = 5;
    private static final String CODE_KEY = "password-reset:" + EMAIL;
    private static final String ATTEMPTS_KEY = "password-reset-attempts:" + EMAIL;

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private PasswordResetCodeService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "resetExpirationMillis", TTL_MS);
        ReflectionTestUtils.setField(service, "maxAttempts", MAX_ATTEMPTS);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("create записывает 6-значный код с TTL и сбрасывает счётчик попыток-успехTest")
    void createStoresCodeWithTtlAndResetsAttemptsTest() {
        String code = service.create(EMAIL);

        assertThat(code).hasSize(6).matches("\\d{6}");
        verify(valueOps).set(eq(CODE_KEY), eq(code), eq(TTL_MS), eq(TimeUnit.MILLISECONDS));
        verify(redisTemplate).delete(ATTEMPTS_KEY);
    }

    @Test
    @DisplayName("verifyAndConsume возвращает true при совпадении и удаляет ключи-успехTest")
    void verifyAndConsumeSucceedsTest() {
        when(valueOps.get(CODE_KEY)).thenReturn("123456");
        when(valueOps.increment(ATTEMPTS_KEY)).thenReturn(1L);

        boolean ok = service.verifyAndConsume(EMAIL, "123456");

        assertThat(ok).isTrue();
        verify(redisTemplate).delete(CODE_KEY);
        verify(redisTemplate).delete(ATTEMPTS_KEY);
    }

    @Test
    @DisplayName("verifyAndConsume возвращает false если кода нет в Redis-ошибкаTest")
    void verifyAndConsumeFailsWhenNoCodeTest() {
        when(valueOps.get(CODE_KEY)).thenReturn(null);

        boolean ok = service.verifyAndConsume(EMAIL, "123456");

        assertThat(ok).isFalse();
        verify(valueOps, never()).increment(any(String.class));
    }

    @Test
    @DisplayName("verifyAndConsume возвращает false и не трогает код при несовпадении-ошибкаTest")
    void verifyAndConsumeFailsOnWrongCodeTest() {
        when(valueOps.get(CODE_KEY)).thenReturn("111111");
        when(valueOps.increment(ATTEMPTS_KEY)).thenReturn(1L);

        boolean ok = service.verifyAndConsume(EMAIL, "222222");

        assertThat(ok).isFalse();
        // Код НЕ удаляется при простом несовпадении — даём ещё попытки.
        verify(redisTemplate, never()).delete(CODE_KEY);
    }

    @Test
    @DisplayName("verifyAndConsume гасит сессию при превышении max-attempts-ошибкаTest")
    void verifyAndConsumeKillsSessionAfterTooManyAttemptsTest() {
        when(valueOps.get(CODE_KEY)).thenReturn("111111");
        when(valueOps.increment(ATTEMPTS_KEY)).thenReturn((long) MAX_ATTEMPTS + 1);

        boolean ok = service.verifyAndConsume(EMAIL, "222222");

        assertThat(ok).isFalse();
        verify(redisTemplate).delete(CODE_KEY);
        verify(redisTemplate).delete(ATTEMPTS_KEY);
    }

    @Test
    @DisplayName("verifyAndConsume на первой попытке ставит TTL счётчика-успехTest")
    void verifyAndConsumeSetsTtlOnFirstAttemptTest() {
        when(valueOps.get(CODE_KEY)).thenReturn("123456");
        when(valueOps.increment(ATTEMPTS_KEY)).thenReturn(1L);

        service.verifyAndConsume(EMAIL, "wrong");

        verify(redisTemplate).expire(eq(ATTEMPTS_KEY), anyLong(), eq(TimeUnit.MILLISECONDS));
    }
}
