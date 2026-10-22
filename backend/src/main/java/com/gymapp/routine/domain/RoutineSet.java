package com.gymapp.routine.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Objectif d'une série fixé par le coach. Ne contient que des valeurs cibles. */
@Entity
@Table(name = "routine_set")
public class RoutineSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routine_exercise_id", nullable = false)
    private RoutineExercise routineExercise;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "set_number", nullable = false)
    private Integer setNumber;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "target_reps", nullable = false)
    private Integer targetReps;

    /** {@code null} = poids du corps. */
    @Column(name = "target_weight_kg", precision = 6, scale = 2)
    private BigDecimal targetWeightKg;

    protected RoutineSet() {
    }

    public Long getId() {
        return id;
    }

    public RoutineExercise getRoutineExercise() {
        return routineExercise;
    }

    public Integer getSetNumber() {
        return setNumber;
    }

    public Integer getTargetReps() {
        return targetReps;
    }

    public BigDecimal getTargetWeightKg() {
        return targetWeightKg;
    }
}
