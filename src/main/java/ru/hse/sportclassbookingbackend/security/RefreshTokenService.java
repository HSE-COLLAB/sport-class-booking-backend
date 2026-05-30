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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${security.refresh.expiration}")
    private Long refreshExpiration;

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String USER_REFRESH_PREFIX = "user-refresh:";

    private final StringRedisTemplate redisTemplate;

    public UUID create(UUID userId, Role role) {
        UUID token = UUID.randomUUID();
        String key = REFRESH_PREFIX + token;
        String indexKey = USER_REFRESH_PREFIX + userId;
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
                    ops.opsForSet().add(indexKey, token.toString());
                    ops.expire(indexKey, refreshExpiration, TimeUnit.MILLISECONDS);
                    return ops.exec();
                } catch (Exception ex) {
                    ops.discard();
                    throw ex;
                }
            }
        });

        return token;
    }

    public Optional<RefreshTokenData> getTokenData(UUID token) {
        String key = REFRESH_PREFIX + token;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        String userIdStr = (String) entries.get("userId");
        String roleStr = (String) entries.get("role");
        if (userIdStr == null || roleStr == null) {
            return Optional.empty();
        }
        return Optional.of(new RefreshTokenData(UUID.fromString(userIdStr), Role.valueOf(roleStr)));
    }

    public void delete(UUID refreshToken) {
        String key = REFRESH_PREFIX + refreshToken;
        String userIdStr = (String) redisTemplate.opsForHash().get(key, "userId");
        redisTemplate.delete(key);
        if (userIdStr != null) {
            redisTemplate.opsForSet().remove(USER_REFRESH_PREFIX + userIdStr, refreshToken.toString());
        }
    }

    public void deleteAllForUser(UUID userId) {
        String indexKey = USER_REFRESH_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(indexKey);
        if (tokens != null && !tokens.isEmpty()) {
            for (String token : tokens) {
                redisTemplate.delete(REFRESH_PREFIX + token);
            }
        }
        redisTemplate.delete(indexKey);
    }

    public record RefreshTokenData(
        UUID userId,
        Role role
    ){
    }
}
