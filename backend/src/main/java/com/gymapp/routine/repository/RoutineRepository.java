package com.gymapp.routine.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.gymapp.routine.domain.Routine;
import com.gymapp.routine.domain.RoutineStatus;

public interface RoutineRepository extends JpaRepository<Routine, Long> {

    /** Routine active du client (au plus une, garanti par l'index unique partiel). */
    @EntityGraph(attributePaths = "workouts")
    Optional<Routine> findByClientIdAndStatus(Long clientId, RoutineStatus status);
}
