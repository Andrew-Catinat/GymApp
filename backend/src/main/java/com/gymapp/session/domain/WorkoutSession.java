package com.gymapp.session.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import com.gymapp.routine.domain.RoutineExercise;
import com.gymapp.routine.domain.RoutineWorkout;
import com.gymapp.user.AppUser;

/**
 * Séance réalisée (ou en cours) par un client : bloc « exécution ».
 * Les objectifs y sont copiés au lancement pour que l'historique ne dépende
 * jamais de l'état courant de la routine.
 */
@Entity
@Table(name = "workout_session")
public class WorkoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private AppUser client;

    /** Séance type d'origine ; peut devenir {@code null} si elle est supprimée. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_workout_id")
    private RoutineWorkout routineWorkout;

    @Column(name = "workout_name", nullable = false)
    private String workoutName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<SessionExercise> exercises = new ArrayList<>();

    protected WorkoutSession() {
    }

    /** Crée une séance en copiant (snapshot) tous les objectifs de la séance type. */
    public static WorkoutSession startFrom(AppUser client, RoutineWorkout workout, Instant now) {
        WorkoutSession session = new WorkoutSession();
        session.client = client;
        session.routineWorkout = workout;
        session.workoutName = workout.getName();
        session.status = SessionStatus.IN_PROGRESS;
        session.startedAt = now;
        for (RoutineExercise routineExercise : workout.getExercises()) {
            session.exercises.add(SessionExercise.snapshotOf(session, routineExercise));
        }
        return session;
    }

    public boolean isInProgress() {
        return status == SessionStatus.IN_PROGRESS;
    }

    /** Clôture la séance ; les séries non cochées restent « non réalisées ». */
    public void complete(Instant now) {
        this.status = SessionStatus.COMPLETED;
        this.endedAt = now;
    }

    public int totalSets() {
        return exercises.stream().mapToInt(e -> e.getSets().size()).sum();
    }

    public int completedSets() {
        return exercises.stream().mapToInt(SessionExercise::completedSets).sum();
    }

    public Long getId() {
        return id;
    }

    public AppUser getClient() {
        return client;
    }

    public RoutineWorkout getRoutineWorkout() {
        return routineWorkout;
    }

    public String getWorkoutName() {
        return workoutName;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public List<SessionExercise> getExercises() {
        return exercises;
    }
}
