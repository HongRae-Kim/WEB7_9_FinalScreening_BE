package com.back.matchduo.domain.auth.refresh.service;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenStore {
    void save(Long userId, String token, Duration ttl);
    Optional<String> findByUserId(Long userId);
    void deleteByUserId(Long userId);

}
