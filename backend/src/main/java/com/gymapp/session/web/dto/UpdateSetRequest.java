package com.gymapp.session.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Saisie d'une série par le client (PUT /api/sessions/{id}/sets/{setId}).
 * {@code actualWeightKg} peut être {@code null} pour un exercice au poids du corps.
 */
public record UpdateSetRequest(
        @Min(0) @Max(999) Integer actualReps,
        @DecimalMin("0.00") @DecimalMax("9999.99") @Digits(integer = 4, fraction = 2) BigDecimal actualWeightKg,
        @NotNull Boolean completed) {

    @AssertTrue(message = "actualReps est obligatoire pour valider une série")
    public boolean isRepsProvidedWhenCompleted() {
        return !Boolean.TRUE.equals(completed) || actualReps != null;
    }
}
