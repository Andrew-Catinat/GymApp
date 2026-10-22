package com.gymapp.session.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gymapp.session.domain.SessionSet;

public interface SessionSetRepository extends JpaRepository<SessionSet, Long> {

    /** Série appartenant à la séance donnée, elle-même appartenant au client donné. */
    @Query("""
            select s from SessionSet s
            join fetch s.sessionExercise se
            join fetch se.session ws
            where s.id = :setId
              and ws.id = :sessionId
              and ws.client.id = :clientId
            """)
    Optional<SessionSet> findOwnedSet(@Param("setId") Long setId,
                                      @Param("sessionId") Long sessionId,
                                      @Param("clientId") Long clientId);
}
