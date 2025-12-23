package com.metrics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MetricsPublisherServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MetricsPublisherServiceApplication.class, args);
	}

}
