package ru.hse.sportclassbookingbackend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ru.hse.sportclassbookingbackend.model.Role;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${security.refresh.expiration}")
    private Long refreshExpiration;

    private static final String REFRESH_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public UUID create(UUID userId, Role role) {
        UUID token = UUID.randomUUID();
        String key = REFRESH_PREFIX + token;
        Map<String, String> fields = new HashMap<>();
        fields.put("userId", userId.toString());
        fields.put("role", role.name());

        redisTemplate.execute(new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public Object execute(RedisOperations ops) {
                ops.multi();
                try {
                    ops.opsForHash().putAll(key, fields);
                    ops.expire(key, refreshExpiration, TimeUnit.MILLISECONDS);
                    return ops.exec();
                } catch (Exception ex) {
                    ops.discard();
                    throw ex;
                }
            }
        });

        return token;
    }

    public Optional<RefreshTokenData> getTokenData(UUID token){
        String key = REFRESH_PREFIX + token;
        if (!redisTemplate.hasKey(key)){
            return Optional.empty();
        }
        UUID userId = UUID.fromString(Objects.requireNonNull(redisTemplate.opsForHash().get(key, "userId")).toString());
        Role role = Role.valueOf(Objects.requireNonNull(redisTemplate.opsForHash().get(key, "role")).toString());

        return Optional.of(new RefreshTokenData(userId,role));
    }

    public void delete(UUID refreshToken){
        String key = REFRESH_PREFIX + refreshToken;
        redisTemplate.delete(key);
    }

    public record RefreshTokenData(
        UUID userId,
        Role role
    ){
    }
}
