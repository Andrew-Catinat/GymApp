package com.gymapp.session.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Une ligne du tableau des séries : prévu (target) et réel (actual). */
public record SessionSetResponse(
        Long id,
        int setNumber,
        int targetReps,
        BigDecimal targetWeightKg,
        Integer actualReps,
        BigDecimal actualWeightKg,
        boolean completed,
        Instant completedAt) {
}
