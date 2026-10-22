package com.gymapp.session.web.dto;

import jakarta.validation.constraints.Positive;

/**
 * Corps (optionnel) de POST /api/sessions/start.
 * Sans {@code routineWorkoutId}, la prochaine séance de la rotation est lancée.
 */
public record StartSessionRequest(@Positive Long routineWorkoutId) {
}
