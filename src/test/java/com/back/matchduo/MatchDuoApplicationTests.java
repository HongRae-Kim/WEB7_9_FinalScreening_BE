package com.back.matchduo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.back.matchduo.support.InMemoryTestConfig;
import org.springframework.context.annotation.Import;

@SpringBootTest
@ActiveProfiles("test")
@Import(InMemoryTestConfig.class)
class MatchDuoApplicationTests {

    @Test
    void contextLoads() {
    }

}
