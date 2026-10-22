package com.gymapp.session.web.dto;

/**
 * Carte « Prochaine séance » du dashboard client.
 * {@code inProgressSessionId} est renseigné si une séance est déjà en cours
 * (le bouton devient alors « Reprendre la séance »).
 */
public record NextWorkoutResponse(
        Long routineId,
        String routineName,
        Long routineWorkoutId,
        String workoutName,
        int position,
        int workoutCount,
        int exerciseCount,
        Long inProgressSessionId) {
}
