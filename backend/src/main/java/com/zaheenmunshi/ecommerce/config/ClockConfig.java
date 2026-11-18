package com.zaheenmunshi.ecommerce.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** One injectable clock so token expiry and lockouts can be tested without sleeping. */
@Configuration
@EnableScheduling
public class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
