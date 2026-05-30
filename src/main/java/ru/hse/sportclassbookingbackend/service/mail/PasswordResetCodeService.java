package ru.hse.sportclassbookingbackend.service.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetCodeService {

    private static final String CODE_PREFIX = "password-reset:";
    private static final String ATTEMPTS_PREFIX = "password-reset-attempts:";

    @Value("${mail.reset.expiration}")
    private Long resetExpirationMillis;

    @Value("${mail.reset.max-attempts}")
    private Integer maxAttempts;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom random = new SecureRandom();

    public String create(String email) {
        String code = generateCode();
        String codeKey = CODE_PREFIX + email;
        String attemptsKey = ATTEMPTS_PREFIX + email;

        redisTemplate.opsForValue().set(codeKey, code, resetExpirationMillis, TimeUnit.MILLISECONDS);
        redisTemplate.delete(attemptsKey);

        log.debug("Password reset code issued for email='{}'", email);
        return code;
    }

    /**
     * @return true если код подошёл и был израсходован; false если код не совпал, истёк или превышен лимит попыток.
     */
    public boolean verifyAndConsume(String email, String code) {
        String codeKey = CODE_PREFIX + email;
        String attemptsKey = ATTEMPTS_PREFIX + email;

        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            return false; // код не запрашивался, истёк или уже использован
        }

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        // Привязываем TTL счётчика к TTL кода, чтобы не висел вечно
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(attemptsKey, resetExpirationMillis, TimeUnit.MILLISECONDS);
        }

        if (attempts != null && attempts > maxAttempts) {
            // Слишком много попыток — гасим всю сессию сброса
            log.warn("Password reset attempts exceeded ({}/{}) for email='{}', session terminated",
                    attempts, maxAttempts, email);
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptsKey);
            return false;
        }

        if (!storedCode.equals(code)) {
            return false;
        }

        // Успех — код и счётчик удаляем (одноразовость)
        redisTemplate.delete(codeKey);
        redisTemplate.delete(attemptsKey);
        return true;
    }

    private String generateCode() {
        int n = 100000 + random.nextInt(900000);
        return Integer.toString(n);
    }
}
