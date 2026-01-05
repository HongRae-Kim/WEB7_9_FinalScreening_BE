package com.back.matchduo.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("JacksonConfig 테스트")
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("LocalDateTime이 오프셋 포함 형식으로 직렬화되어야 한다")
    void localDateTime_shouldBeSerializedWithOffset() throws Exception {
        // given
        LocalDateTime testTime = LocalDateTime.of(2026, 1, 5, 12, 4, 33, 824913000);
        TestDto dto = new TestDto(testTime);

        // when
        String json = objectMapper.writeValueAsString(dto);

        // then
        System.out.println("직렬화 결과: " + json);

        // +09:00 오프셋이 포함되어야 함
        assertThat(json).contains("+09:00");
        assertThat(json).contains("2026-01-05T12:04:33");
    }

    record TestDto(LocalDateTime createdAt) {}
}