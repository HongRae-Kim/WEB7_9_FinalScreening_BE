package com.back.matchduo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.back.matchduo")
public class MatchDuoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchDuoApplication.class, args);
    }

}
