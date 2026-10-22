package com.gymapp.session.web.dto;

import java.time.Instant;
import java.util.List;

import com.gymapp.session.domain.SessionStatus;

/** Séance complète, telle qu'affichée pendant l'exécution. */
public record SessionResponse(
        Long id,
        String workoutName,
        SessionStatus status,
        Instant startedAt,
        Instant endedAt,
        int totalSets,
        int completedSets,
        List<SessionExerciseResponse> exercises) {
}
