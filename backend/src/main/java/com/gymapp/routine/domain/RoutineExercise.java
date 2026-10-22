package com.gymapp.routine.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.gymapp.exercise.Exercise;

/** Exercice prescrit dans une séance type : ordre, repos et conseil du coach. */
@Entity
@Table(name = "routine_exercise")
public class RoutineExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routine_workout_id", nullable = false)
    private RoutineWorkout routineWorkout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false)
    private Integer position;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "rest_seconds", nullable = false)
    private Integer restSeconds;

    @Column(name = "coach_tip")
    private String coachTip;

    @OneToMany(mappedBy = "routineExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("setNumber ASC")
    private List<RoutineSet> sets = new ArrayList<>();

    protected RoutineExercise() {
    }

    public Long getId() {
        return id;
    }

    public RoutineWorkout getRoutineWorkout() {
        return routineWorkout;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public Integer getPosition() {
        return position;
    }

    public Integer getRestSeconds() {
        return restSeconds;
    }

    public String getCoachTip() {
        return coachTip;
    }

    public List<RoutineSet> getSets() {
        return sets;
    }
}
