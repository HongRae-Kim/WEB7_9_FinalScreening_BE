package com.back.matchduo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatUnreadCacheService {

    private final RedisTemplate<String, Long> redisTemplate;

    private static final String UNREAD_KEY_PREFIX = "chat:unread:";
    private static final Duration TTL = Duration.ofDays(7);

    private String buildKey(Long chatRoomId, Long userId) {
        return UNREAD_KEY_PREFIX + chatRoomId + ":" + userId;
    }

    /** 메시지 전송 시 상대방 unread +1 **/
    public void increment(Long chatRoomId, Long userId) {
        try {
            String key = buildKey(chatRoomId, userId);
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, TTL);
            log.debug("Redis unread 증가: key={}", key);
        } catch (Exception e) {
            log.warn("Redis increment 실패: {}", e.getMessage());
        }
    }

    /** 읽음 처리 시 unread = 0 **/
    public void reset(Long chatRoomId, Long userId) {
        try {
            String key = buildKey(chatRoomId, userId);
            redisTemplate.opsForValue().set(key, 0L, TTL);
            log.debug("Redis unread 초기화: key={}", key);
        } catch (Exception e) {
            log.warn("Redis reset 실패: {}", e.getMessage());
        }
    }

    /** 조회: Redis 없으면 DB fallback **/
    public int getOrSync(Long chatRoomId, Long userId, Supplier<Long> dbCounter) {
        try {
            String key = buildKey(chatRoomId, userId);
            Long count = redisTemplate.opsForValue().get(key);

            if (count == null) {
                long dbCount = dbCounter.get();
                redisTemplate.opsForValue().set(key, dbCount, TTL);
                return (int) dbCount;
            }
            return count.intValue();
        } catch (Exception e) {
            log.warn("Redis 조회 실패, DB fallback: {}", e.getMessage());
            return dbCounter.get().intValue();
        }
    }

    /** 키 삭제 (채팅방 닫힐 때) **/
    public void delete(Long chatRoomId, Long userId) {
        try {
            redisTemplate.delete(buildKey(chatRoomId, userId));
        } catch (Exception e) {
            log.warn("Redis delete 실패: {}", e.getMessage());
        }
    }

    /** 채팅방 관련 모든 키 삭제 (스케줄러용) **/
    public void deleteByChatRoomId(Long chatRoomId) {
        try {
            String pattern = UNREAD_KEY_PREFIX + chatRoomId + ":*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Redis 채팅방 캐시 삭제: chatRoomId={}, count={}", chatRoomId, keys.size());
            }
        } catch (Exception e) {
            log.warn("Redis deleteByChatRoomId 실패: {}", e.getMessage());
        }
    }
}
