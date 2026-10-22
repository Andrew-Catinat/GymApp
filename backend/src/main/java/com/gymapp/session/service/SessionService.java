package com.gymapp.session.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gymapp.common.exception.BusinessException;
import com.gymapp.common.exception.ErrorCode;
import com.gymapp.common.exception.ResourceNotFoundException;
import com.gymapp.routine.domain.Routine;
import com.gymapp.routine.domain.RoutineStatus;
import com.gymapp.routine.domain.RoutineWorkout;
import com.gymapp.routine.repository.RoutineRepository;
import com.gymapp.routine.repository.RoutineWorkoutRepository;
import com.gymapp.session.domain.SessionSet;
import com.gymapp.session.domain.SessionStatus;
import com.gymapp.session.domain.WorkoutSession;
import com.gymapp.session.repository.SessionSetRepository;
import com.gymapp.session.repository.WorkoutSessionRepository;
import com.gymapp.session.web.dto.NextWorkoutResponse;
import com.gymapp.session.web.dto.SessionResponse;
import com.gymapp.session.web.dto.SessionSetResponse;
import com.gymapp.session.web.dto.SessionSummaryResponse;
import com.gymapp.session.web.dto.StartSessionRequest;
import com.gymapp.session.web.dto.UpdateSetRequest;
import com.gymapp.user.AppUserRepository;

/**
 * Exécution d'une séance par un client : lancement (copie des objectifs),
 * saisie des séries en temps réel et clôture.
 * Toutes les méthodes reçoivent l'id du client authentifié : une séance
 * appartenant à un autre client est traitée comme introuvable (404).
 */
@Service
@Transactional
public class SessionService {

    private final WorkoutSessionRepository sessions;
    private final SessionSetRepository sets;
    private final RoutineRepository routines;
    private final RoutineWorkoutRepository routineWorkouts;
    private final AppUserRepository users;
    private final SessionMapper mapper;
    private final Clock clock;

    public SessionService(WorkoutSessionRepository sessions,
                          SessionSetRepository sets,
                          RoutineRepository routines,
                          RoutineWorkoutRepository routineWorkouts,
                          AppUserRepository users,
                          SessionMapper mapper,
                          Clock clock) {
        this.sessions = sessions;
        this.sets = sets;
        this.routines = routines;
        this.routineWorkouts = routineWorkouts;
        this.users = users;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** Prochaine séance à réaliser, pour le dashboard. */
    @Transactional(readOnly = true)
    public NextWorkoutResponse getNextWorkout(Long clientId) {
        Routine routine = requireActiveRoutine(clientId);
        RoutineWorkout next = nextWorkoutInRotation(routine, clientId);
        Long inProgressId = sessions.findFirstByClientIdAndStatus(clientId, SessionStatus.IN_PROGRESS)
                .map(WorkoutSession::getId)
                .orElse(null);
        return new NextWorkoutResponse(
                routine.getId(),
                routine.getName(),
                next.getId(),
                next.getName(),
                next.getPosition(),
                routine.getWorkouts().size(),
                next.getExercises().size(),
                inProgressId);
    }

    /** Lance une séance : copie des objectifs de la séance type dans les tables d'exécution. */
    public SessionResponse start(Long clientId, StartSessionRequest request) {
        sessions.findFirstByClientIdAndStatus(clientId, SessionStatus.IN_PROGRESS)
                .ifPresent(current -> {
                    throw new BusinessException(ErrorCode.SESSION_ALREADY_IN_PROGRESS,
                            "Une séance est déjà en cours.",
                            Map.of("sessionId", current.getId()));
                });

        Routine routine = requireActiveRoutine(clientId);
        Long workoutId = request != null && request.routineWorkoutId() != null
                ? request.routineWorkoutId()
                : nextWorkoutInRotation(routine, clientId).getId();

        RoutineWorkout workout = routineWorkouts.findForSnapshot(workoutId)
                .filter(w -> w.getRoutine().getId().equals(routine.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKOUT_NOT_IN_ACTIVE_ROUTINE,
                        "Cette séance type n'appartient pas à la routine active.",
                        Map.of("routineWorkoutId", workoutId)));
        if (workout.getExercises().isEmpty()) {
            throw new BusinessException(ErrorCode.WORKOUT_HAS_NO_EXERCISE,
                    "Cette séance type ne contient aucun exercice.");
        }

        WorkoutSession session = WorkoutSession.startFrom(
                users.getReferenceById(clientId), workout, Instant.now(clock));
        // saveAndFlush : l'index unique « une séance en cours par client » est
        // vérifié tout de suite, même en cas de double clic simultané.
        return mapper.toResponse(sessions.saveAndFlush(session));
    }

    /** Séance en cours du client, s'il y en a une (reprise après fermeture de l'app). */
    @Transactional(readOnly = true)
    public Optional<SessionResponse> findCurrent(Long clientId) {
        return sessions.findFirstByClientIdAndStatus(clientId, SessionStatus.IN_PROGRESS)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SessionResponse get(Long clientId, Long sessionId) {
        return mapper.toResponse(requireSession(clientId, sessionId));
    }

    /** Saisie d'une série (reps/poids réels + case cochée), appelée à chaque modification. */
    public SessionSetResponse updateSet(Long clientId, Long sessionId, Long setId, UpdateSetRequest request) {
        SessionSet set = sets.findOwnedSet(setId, sessionId, clientId)
                .orElseThrow(() -> {
                    requireSession(clientId, sessionId); // distingue séance introuvable / série introuvable
                    return new ResourceNotFoundException("Série", setId);
                });
        ensureInProgress(set.getSessionExercise().getSession());
        set.record(request.actualReps(), request.actualWeightKg(), request.completed(), Instant.now(clock));
        return mapper.toResponse(set);
    }

    /**
     * Clôture la séance (boutons « Terminer la séance » et « Mettre fin à la séance »).
     * Les séries non cochées restent non réalisées et apparaîtront comme telles dans l'historique.
     */
    public SessionSummaryResponse complete(Long clientId, Long sessionId) {
        WorkoutSession session = requireSession(clientId, sessionId);
        ensureInProgress(session);
        session.complete(Instant.now(clock));
        return mapper.toSummary(session);
    }

    private WorkoutSession requireSession(Long clientId, Long sessionId) {
        return sessions.findByIdAndClientId(sessionId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Séance", sessionId));
    }

    private void ensureInProgress(WorkoutSession session) {
        if (!session.isInProgress()) {
            throw new BusinessException(ErrorCode.SESSION_NOT_IN_PROGRESS,
                    "La séance est déjà terminée.",
                    Map.of("sessionId", session.getId(), "sessionStatus", session.getStatus().name()));
        }
    }

    private Routine requireActiveRoutine(Long clientId) {
        Routine routine = routines.findByClientIdAndStatus(clientId, RoutineStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_ACTIVE_ROUTINE,
                        "Aucune routine active : votre coach doit d'abord vous en attribuer une."));
        if (routine.getWorkouts().isEmpty()) {
            throw new BusinessException(ErrorCode.ROUTINE_HAS_NO_WORKOUT,
                    "La routine active ne contient aucune séance.");
        }
        return routine;
    }

    /** Rotation : la séance qui suit la dernière terminée, puis retour à la première. */
    RoutineWorkout nextWorkoutInRotation(Routine routine, Long clientId) {
        List<RoutineWorkout> workouts = routine.getWorkouts();
        int lastPosition = sessions.findLastCompletedPosition(clientId, routine.getId()).orElse(0);
        return workouts.stream()
                .filter(w -> w.getPosition() > lastPosition)
                .findFirst()
                .orElse(workouts.getFirst());
    }
}
