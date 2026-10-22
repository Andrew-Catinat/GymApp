package com.gymapp.session.web.dto;

import java.time.Instant;

import com.gymapp.session.domain.SessionStatus;

/** Résumé renvoyé à la clôture d'une séance. */
public record SessionSummaryResponse(
        Long id,
        String workoutName,
        SessionStatus status,
        Instant startedAt,
        Instant endedAt,
        long durationSeconds,
        int totalSets,
        int completedSets) {
}
