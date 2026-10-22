package com.gymapp.session.web.dto;

/** Informations d'affichage d'un exercice (vignette + animation jouée au clic). */
public record ExerciseInfo(
        Long id,
        String name,
        String bodyPart,
        String target,
        String equipment,
        String imageUrl,
        String animationUrl) {
}
