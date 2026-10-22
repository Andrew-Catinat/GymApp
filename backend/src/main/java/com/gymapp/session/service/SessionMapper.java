package com.gymapp.session.service;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.gymapp.exercise.Exercise;
import com.gymapp.exercise.MediaUrlResolver;
import com.gymapp.session.domain.SessionExercise;
import com.gymapp.session.domain.SessionSet;
import com.gymapp.session.domain.WorkoutSession;
import com.gymapp.session.web.dto.ExerciseInfo;
import com.gymapp.session.web.dto.SessionExerciseResponse;
import com.gymapp.session.web.dto.SessionResponse;
import com.gymapp.session.web.dto.SessionSetResponse;
import com.gymapp.session.web.dto.SessionSummaryResponse;

/** Conversion entités -> DTO. Les entités ne sortent jamais de la couche service. */
@Component
public class SessionMapper {

    private final MediaUrlResolver media;

    public SessionMapper(MediaUrlResolver media) {
        this.media = media;
    }

    public SessionResponse toResponse(WorkoutSession session) {
        return new SessionResponse(
                session.getId(),
                session.getWorkoutName(),
                session.getStatus(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.totalSets(),
                session.completedSets(),
                session.getExercises().stream().map(this::toResponse).toList());
    }

    public SessionExerciseResponse toResponse(SessionExercise exercise) {
        return new SessionExerciseResponse(
                exercise.getId(),
                exercise.getPosition(),
                toInfo(exercise.getExercise()),
                exercise.getRestSeconds(),
                exercise.getCoachTip(),
                exercise.isCompleted(),
                exercise.getSets().stream().map(this::toResponse).toList());
    }

    public SessionSetResponse toResponse(SessionSet set) {
        return new SessionSetResponse(
                set.getId(),
                set.getSetNumber(),
                set.getTargetReps(),
                set.getTargetWeightKg(),
                set.getActualReps(),
                set.getActualWeightKg(),
                set.isCompleted(),
                set.getCompletedAt());
    }

    public SessionSummaryResponse toSummary(WorkoutSession session) {
        long duration = session.getEndedAt() == null ? 0
                : Duration.between(session.getStartedAt(), session.getEndedAt()).toSeconds();
        return new SessionSummaryResponse(
                session.getId(),
                session.getWorkoutName(),
                session.getStatus(),
                session.getStartedAt(),
                session.getEndedAt(),
                duration,
                session.totalSets(),
                session.completedSets());
    }

    private ExerciseInfo toInfo(Exercise exercise) {
        return new ExerciseInfo(
                exercise.getId(),
                exercise.getName(),
                exercise.getBodyPart(),
                exercise.getTarget(),
                exercise.getEquipment(),
                media.resolve(exercise.getImagePath()),
                media.resolve(exercise.getGifPath()));
    }
}
