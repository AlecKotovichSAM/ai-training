package com.alec.aitraining;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AitrainingApplication {

	public static void main(String[] args) {
		SpringApplication.run(AitrainingApplication.class, args);
	}
}
