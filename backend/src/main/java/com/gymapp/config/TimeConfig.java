package com.gymapp.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    /** Horloge injectable : permet de figer le temps dans les tests. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
