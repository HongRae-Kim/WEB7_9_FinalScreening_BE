package com.back.matchduo.support;

import com.back.matchduo.domain.auth.refresh.service.RefreshTokenStore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Profile("test")
public class InMemoryTestConfig {

    @Bean
    @Primary
    public RefreshTokenStore refreshTokenStore() {
        return new RefreshTokenStore() {
            private final Map<Long, String> store = new ConcurrentHashMap<>();

            @Override
            public void save(Long userId, String token, Duration ttl) {
                store.put(userId, token);
            }

            @Override
            public Optional<String> findByUserId(Long userId) {
                return Optional.ofNullable(store.get(userId));
            }

            @Override
            public void deleteByUserId(Long userId) {
                store.remove(userId);
            }
        };
    }
}
