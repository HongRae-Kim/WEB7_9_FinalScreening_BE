package com.back.matchduo.domain.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatUnreadCacheService 테스트")
class ChatUnreadCacheServiceTest {

    @Mock
    private RedisTemplate<String, Long> redisTemplate;

    @Mock
    private ValueOperations<String, Long> valueOperations;

    @InjectMocks
    private ChatUnreadCacheService chatUnreadCacheService;

    private static final Long CHAT_ROOM_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final String EXPECTED_KEY = "chat:unread:1:100";

    @Nested
    @DisplayName("increment 메서드")
    class IncrementTest {

        @BeforeEach
        void setUp() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
        }

        @Test
        @DisplayName("unread 카운트 증가 성공")
        void increment_success() {
            // given
            given(valueOperations.increment(EXPECTED_KEY)).willReturn(1L);
            given(redisTemplate.expire(eq(EXPECTED_KEY), any(Duration.class))).willReturn(true);

            // when
            chatUnreadCacheService.increment(CHAT_ROOM_ID, USER_ID);

            // then
            verify(valueOperations).increment(EXPECTED_KEY);
            verify(redisTemplate).expire(eq(EXPECTED_KEY), any(Duration.class));
        }

        @Test
        @DisplayName("Redis 연결 실패 시 예외를 삼키고 정상 종료")
        void increment_fail_gracefully() {
            // given
            given(valueOperations.increment(anyString())).willThrow(new RuntimeException("Redis 연결 실패"));

            // when - 예외가 발생하지 않아야 함
            chatUnreadCacheService.increment(CHAT_ROOM_ID, USER_ID);

            // then - 예외 없이 정상 종료 확인
            verify(valueOperations).increment(EXPECTED_KEY);
        }
    }

    @Nested
    @DisplayName("reset 메서드")
    class ResetTest {

        @BeforeEach
        void setUp() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
        }

        @Test
        @DisplayName("unread 카운트 초기화 성공")
        void reset_success() {
            // when
            chatUnreadCacheService.reset(CHAT_ROOM_ID, USER_ID);

            // then
            verify(valueOperations).set(eq(EXPECTED_KEY), eq(0L), any(Duration.class));
        }

        @Test
        @DisplayName("Redis 연결 실패 시 예외를 삼키고 정상 종료")
        void reset_fail_gracefully() {
            // given
            org.mockito.Mockito.doThrow(new RuntimeException("Redis 연결 실패"))
                    .when(valueOperations).set(anyString(), anyLong(), any(Duration.class));

            // when - 예외가 발생하지 않아야 함
            chatUnreadCacheService.reset(CHAT_ROOM_ID, USER_ID);

            // then - 예외 없이 정상 종료 확인
            verify(valueOperations).set(eq(EXPECTED_KEY), eq(0L), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("getOrSync 메서드")
    class GetOrSyncTest {

        @BeforeEach
        void setUp() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
        }

        @Test
        @DisplayName("Redis에 캐시가 있으면 캐시값 반환")
        void getOrSync_cache_hit() {
            // given
            given(valueOperations.get(EXPECTED_KEY)).willReturn(5L);

            // when
            int result = chatUnreadCacheService.getOrSync(CHAT_ROOM_ID, USER_ID, () -> 10L);

            // then
            assertThat(result).isEqualTo(5);
            verify(valueOperations).get(EXPECTED_KEY);
            verify(valueOperations, never()).set(anyString(), anyLong(), any(Duration.class));
        }

        @Test
        @DisplayName("Redis에 캐시가 없으면 DB fallback 후 캐시 저장")
        void getOrSync_cache_miss() {
            // given
            given(valueOperations.get(EXPECTED_KEY)).willReturn(null);

            // when
            int result = chatUnreadCacheService.getOrSync(CHAT_ROOM_ID, USER_ID, () -> 3L);

            // then
            assertThat(result).isEqualTo(3);
            verify(valueOperations).get(EXPECTED_KEY);
            verify(valueOperations).set(eq(EXPECTED_KEY), eq(3L), any(Duration.class));
        }

        @Test
        @DisplayName("Redis 연결 실패 시 DB fallback 값 반환")
        void getOrSync_fail_fallback() {
            // given
            given(valueOperations.get(anyString())).willThrow(new RuntimeException("Redis 연결 실패"));

            // when
            int result = chatUnreadCacheService.getOrSync(CHAT_ROOM_ID, USER_ID, () -> 7L);

            // then
            assertThat(result).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("delete 메서드")
    class DeleteTest {

        @Test
        @DisplayName("키 삭제 성공")
        void delete_success() {
            // given
            given(redisTemplate.delete(EXPECTED_KEY)).willReturn(true);

            // when
            chatUnreadCacheService.delete(CHAT_ROOM_ID, USER_ID);

            // then
            verify(redisTemplate).delete(EXPECTED_KEY);
        }

        @Test
        @DisplayName("Redis 연결 실패 시 예외를 삼키고 정상 종료")
        void delete_fail_gracefully() {
            // given
            given(redisTemplate.delete(anyString())).willThrow(new RuntimeException("Redis 연결 실패"));

            // when
            chatUnreadCacheService.delete(CHAT_ROOM_ID, USER_ID);

            // then
            verify(redisTemplate).delete(EXPECTED_KEY);
        }
    }

    @Nested
    @DisplayName("deleteByChatRoomId 메서드")
    class DeleteByChatRoomIdTest {

        @Test
        @DisplayName("채팅방 관련 모든 키 삭제 성공")
        void deleteByChatRoomId_success() {
            // given
            String pattern = "chat:unread:1:*";
            Set<String> keys = Set.of("chat:unread:1:100", "chat:unread:1:200");
            given(redisTemplate.keys(pattern)).willReturn(keys);
            given(redisTemplate.delete(keys)).willReturn(2L);

            // when
            chatUnreadCacheService.deleteByChatRoomId(CHAT_ROOM_ID);

            // then
            verify(redisTemplate).keys(pattern);
            verify(redisTemplate).delete(keys);
        }

        @Test
        @DisplayName("삭제할 키가 없으면 delete 호출 안함")
        void deleteByChatRoomId_no_keys() {
            // given
            String pattern = "chat:unread:1:*";
            given(redisTemplate.keys(pattern)).willReturn(Set.of());

            // when
            chatUnreadCacheService.deleteByChatRoomId(CHAT_ROOM_ID);

            // then
            verify(redisTemplate).keys(pattern);
            verify(redisTemplate, never()).delete(any(Set.class));
        }

        @Test
        @DisplayName("Redis 연결 실패 시 예외를 삼키고 정상 종료")
        void deleteByChatRoomId_fail_gracefully() {
            // given
            given(redisTemplate.keys(anyString())).willThrow(new RuntimeException("Redis 연결 실패"));

            // when
            chatUnreadCacheService.deleteByChatRoomId(CHAT_ROOM_ID);

            // then
            verify(redisTemplate).keys("chat:unread:1:*");
        }
    }
}