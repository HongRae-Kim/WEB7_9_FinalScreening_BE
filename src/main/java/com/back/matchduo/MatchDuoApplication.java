package com.back.matchduo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class MatchDuoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchDuoApplication.class, args);
    }

}
