package com.gymapp.routine.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gymapp.routine.domain.RoutineWorkout;

public interface RoutineWorkoutRepository extends JpaRepository<RoutineWorkout, Long> {

    /**
     * Charge une séance type avec ses exercices et le catalogue associé,
     * prête à être copiée dans une séance d'exécution. Les séries sont
     * chargées par lot (hibernate.default_batch_fetch_size).
     */
    @Query("""
            select distinct rw from RoutineWorkout rw
            join fetch rw.routine r
            left join fetch rw.exercises re
            left join fetch re.exercise
            where rw.id = :id
            """)
    Optional<RoutineWorkout> findForSnapshot(@Param("id") Long id);
}
