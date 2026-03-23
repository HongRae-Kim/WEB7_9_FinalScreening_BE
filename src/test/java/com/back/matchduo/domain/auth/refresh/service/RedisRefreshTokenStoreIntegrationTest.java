package com.back.matchduo.domain.auth.refresh.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataRedisTest
@Testcontainers
@Import(RedisRefreshTokenStore.class)
@DisplayName("RedisRefreshTokenStore 통합 테스트")
class RedisRefreshTokenStoreIntegrationTest {

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @AfterEach
    void tearDown() {
        stringRedisTemplate.delete("auth:refresh:1");
    }

    @Test
    @DisplayName("save 후 findByUserId로 조회할 수 있다")
    void save_and_find() {
        refreshTokenStore.save(1L, "token-123", Duration.ofMinutes(10));

        assertThat(refreshTokenStore.findByUserId(1L)).contains("token-123");
    }

    @Test
    @DisplayName("deleteByUserId 후 조회되지 않는다")
    void delete() {
        refreshTokenStore.save(1L, "token-123", Duration.ofMinutes(10));

        refreshTokenStore.deleteByUserId(1L);

        assertThat(refreshTokenStore.findByUserId(1L)).isEmpty();
    }

    @Test
    @DisplayName("TTL이 지나면 토큰이 만료된다")
    void ttl_expire() throws Exception {
        refreshTokenStore.save(1L, "token-123", Duration.ofSeconds(1));

        Thread.sleep(1500);

        assertThat(refreshTokenStore.findByUserId(1L)).isEmpty();
    }
}
