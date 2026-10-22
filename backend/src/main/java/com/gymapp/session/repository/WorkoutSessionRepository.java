package com.gymapp.session.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gymapp.session.domain.SessionStatus;
import com.gymapp.session.domain.WorkoutSession;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {

    /** Séance d'un client donné : un client ne voit jamais la séance d'un autre. */
    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    Optional<WorkoutSession> findByIdAndClientId(Long id, Long clientId);

    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    Optional<WorkoutSession> findFirstByClientIdAndStatus(Long clientId, SessionStatus status);

    /**
     * Position (dans la routine) de la dernière séance terminée du client,
     * utilisée pour déterminer la prochaine séance de la rotation.
     */
    @Query("""
            select rw.position from WorkoutSession ws
            join ws.routineWorkout rw
            where ws.client.id = :clientId
              and rw.routine.id = :routineId
              and ws.status = com.gymapp.session.domain.SessionStatus.COMPLETED
            order by ws.startedAt desc
            limit 1
            """)
    Optional<Integer> findLastCompletedPosition(@Param("clientId") Long clientId,
                                                @Param("routineId") Long routineId);
}
