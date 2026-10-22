package com.gymapp.session.web.dto;

import java.util.List;

/** Un onglet de la séance. {@code completed} = toutes ses séries sont cochées. */
public record SessionExerciseResponse(
        Long id,
        int position,
        ExerciseInfo exercise,
        int restSeconds,
        String coachTip,
        boolean completed,
        List<SessionSetResponse> sets) {
}
