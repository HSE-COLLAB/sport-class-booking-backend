package ru.hse.sportclassbookingbackend.service.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationTokenService {

    @Value("${mail.verification.expiration}")
    private Long verificationExpiration;

    private static final String VERIFY_PREFIX = "email-verify:";

    private final StringRedisTemplate redisTemplate;

    public UUID create(UUID userId) {
        UUID token = UUID.randomUUID();
        String key = VERIFY_PREFIX + token;
        redisTemplate.opsForValue().set(key, userId.toString(), verificationExpiration, TimeUnit.MILLISECONDS);
        log.debug("Email verification token issued for userId={}", userId);
        return token;
    }

    public Optional<UUID> consume(UUID token) {
        String key = VERIFY_PREFIX + token;
        String userIdStr = redisTemplate.opsForValue().getAndDelete(key);
        if (userIdStr == null) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(userIdStr));
    }
}
