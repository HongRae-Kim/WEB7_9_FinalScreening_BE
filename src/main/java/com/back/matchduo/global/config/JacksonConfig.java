package com.back.matchduo.global.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 직렬화 설정
 * - LocalDateTime을 ISO-8601 with timezone 형식으로 직렬화
 * - 예: 2026-01-05T12:04:33.824913+09:00
 */
@Configuration
public class JacksonConfig {

    private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.serializers(new LocalDateTimeWithOffsetSerializer());
    }

    /**
     * LocalDateTime을 오프셋 포함 형식으로 직렬화하는 Serializer
     */
    public static class LocalDateTimeWithOffsetSerializer extends JsonSerializer<LocalDateTime> {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            // LocalDateTime에 서버 타임존(Asia/Seoul) 적용 후 오프셋 포함 형식으로 직렬화
            String formatted = value.atZone(SERVER_ZONE).format(FORMATTER);
            gen.writeString(formatted);
        }

        @Override
        public Class<LocalDateTime> handledType() {
            return LocalDateTime.class;
        }
    }
}