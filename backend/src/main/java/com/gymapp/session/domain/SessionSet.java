package com.gymapp.session.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

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

import com.gymapp.routine.domain.RoutineSet;

/**
 * Série d'une séance réalisée.
 * {@code target*} : objectifs copiés au lancement, jamais modifiés ensuite.
 * {@code actual*} : valeurs réelles saisies par le client.
 */
@Entity
@Table(name = "session_set")
public class SessionSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_exercise_id", nullable = false)
    private SessionExercise sessionExercise;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "set_number", nullable = false)
    private Integer setNumber;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "target_reps", nullable = false, updatable = false)
    private Integer targetReps;

    @Column(name = "target_weight_kg", precision = 6, scale = 2, updatable = false)
    private BigDecimal targetWeightKg;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "actual_reps")
    private Integer actualReps;

    @Column(name = "actual_weight_kg", precision = 6, scale = 2)
    private BigDecimal actualWeightKg;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected SessionSet() {
    }

    /** Copie l'objectif et préremplit le réel avec la cible : le client n'a qu'à cocher. */
    static SessionSet snapshotOf(SessionExercise sessionExercise, RoutineSet source) {
        SessionSet copy = new SessionSet();
        copy.sessionExercise = sessionExercise;
        copy.setNumber = source.getSetNumber();
        copy.targetReps = source.getTargetReps();
        copy.targetWeightKg = source.getTargetWeightKg();
        copy.actualReps = source.getTargetReps();
        copy.actualWeightKg = source.getTargetWeightKg();
        copy.completed = false;
        return copy;
    }

    /** Enregistre la saisie du client ; décocher une série efface sa date de validation. */
    public void record(Integer actualReps, BigDecimal actualWeightKg, boolean completed, Instant now) {
        this.actualReps = actualReps;
        this.actualWeightKg = actualWeightKg == null ? null : actualWeightKg.setScale(2, RoundingMode.HALF_UP);
        if (completed && !this.completed) {
            this.completedAt = now;
        } else if (!completed) {
            this.completedAt = null;
        }
        this.completed = completed;
    }

    public Long getId() {
        return id;
    }

    public SessionExercise getSessionExercise() {
        return sessionExercise;
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

    public Integer getActualReps() {
        return actualReps;
    }

    public BigDecimal getActualWeightKg() {
        return actualWeightKg;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
