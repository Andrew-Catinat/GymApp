package com.gymapp.exercise;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** URL de base des médias (vignettes JPG et GIF animés) du dataset d'exercices. */
@ConfigurationProperties(prefix = "gymapp.media")
public record MediaProperties(String baseUrl) {
}
