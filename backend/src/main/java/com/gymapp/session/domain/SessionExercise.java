package com.gymapp.session.domain;

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
import com.gymapp.routine.domain.RoutineExercise;
import com.gymapp.routine.domain.RoutineSet;

/** Exercice d'une séance réalisée, avec le repos et le conseil figés au lancement. */
@Entity
@Table(name = "session_exercise")
public class SessionExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private WorkoutSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_exercise_id")
    private RoutineExercise routineExercise;

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

    @OneToMany(mappedBy = "sessionExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("setNumber ASC")
    private List<SessionSet> sets = new ArrayList<>();

    protected SessionExercise() {
    }

    static SessionExercise snapshotOf(WorkoutSession session, RoutineExercise source) {
        SessionExercise copy = new SessionExercise();
        copy.session = session;
        copy.routineExercise = source;
        copy.exercise = source.getExercise();
        copy.position = source.getPosition();
        copy.restSeconds = source.getRestSeconds();
        copy.coachTip = source.getCoachTip();
        for (RoutineSet routineSet : source.getSets()) {
            copy.sets.add(SessionSet.snapshotOf(copy, routineSet));
        }
        return copy;
    }

    public boolean isCompleted() {
        return !sets.isEmpty() && sets.stream().allMatch(SessionSet::isCompleted);
    }

    public int completedSets() {
        return (int) sets.stream().filter(SessionSet::isCompleted).count();
    }

    public Long getId() {
        return id;
    }

    public WorkoutSession getSession() {
        return session;
    }

    public RoutineExercise getRoutineExercise() {
        return routineExercise;
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

    public List<SessionSet> getSets() {
        return sets;
    }
}
