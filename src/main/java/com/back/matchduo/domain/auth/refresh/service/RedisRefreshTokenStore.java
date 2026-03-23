package com.back.matchduo.domain.auth.refresh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(Long userId, String token, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key(userId), token, ttl);
    }

    @Override
    public Optional<String> findByUserId(Long userId) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(key(userId)));
    }

    @Override
    public void deleteByUserId(Long userId) {
        stringRedisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
