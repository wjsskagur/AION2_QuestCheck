package com.aion2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Aion2QuestApplication {
    public static void main(String[] args) {
        SpringApplication.run(Aion2QuestApplication.class, args);
    }
}
