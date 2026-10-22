package com.gymapp.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parcours complet sur une vraie base PostgreSQL (celle de application.yml).
 * Désactivé par défaut : lancer avec la variable d'environnement GYMAPP_IT=true.
 * Chaque test est annulé (rollback) à la fin : la base n'est pas modifiée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "GYMAPP_IT", matches = "true")
class SessionFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long clientId;
    private long otherClientId;
    private long workoutA;
    private long workoutB;

    @BeforeEach
    void seed() {
        long coachId = insert("""
                INSERT INTO app_user (email, password_hash, role, first_name, last_name)
                VALUES ('it-coach@gymapp.test', 'x', 'COACH', 'Coach', 'Test') RETURNING id""");
        clientId = insert("""
                INSERT INTO app_user (email, password_hash, role, first_name, last_name, coach_id)
                VALUES ('it-client@gymapp.test', 'x', 'CLIENT', 'Client', 'Test', ?) RETURNING id""", coachId);
        otherClientId = insert("""
                INSERT INTO app_user (email, password_hash, role, first_name, last_name, coach_id)
                VALUES ('it-other@gymapp.test', 'x', 'CLIENT', 'Autre', 'Test', ?) RETURNING id""", coachId);
        long bench = insert("""
                INSERT INTO exercise (external_id, name, body_part, image_path, gif_path)
                VALUES ('IT-1', 'bench press', 'chest', 'images/it-1.jpg', 'videos/it-1.gif') RETURNING id""");
        long squat = insert("""
                INSERT INTO exercise (external_id, name, body_part, image_path, gif_path)
                VALUES ('IT-2', 'squat', 'upper legs', 'images/it-2.jpg', 'videos/it-2.gif') RETURNING id""");
        long routine = insert("""
                INSERT INTO routine (client_id, coach_id, name, status)
                VALUES (?, ?, 'Force', 'ACTIVE') RETURNING id""", clientId, coachId);
        workoutA = insert("INSERT INTO routine_workout (routine_id, position, name) VALUES (?, 1, 'Jour A') RETURNING id", routine);
        workoutB = insert("INSERT INTO routine_workout (routine_id, position, name) VALUES (?, 2, 'Jour B') RETURNING id", routine);
        long benchA = insert("""
                INSERT INTO routine_exercise (routine_workout_id, exercise_id, position, rest_seconds, coach_tip)
                VALUES (?, ?, 1, 90, 'Omoplates serrées') RETURNING id""", workoutA, bench);
        long squatB = insert("""
                INSERT INTO routine_exercise (routine_workout_id, exercise_id, position, rest_seconds)
                VALUES (?, ?, 1, 120) RETURNING id""", workoutB, squat);
        jdbc.update("INSERT INTO routine_set (routine_exercise_id, set_number, target_reps, target_weight_kg) VALUES (?, 1, 10, 60), (?, 2, 8, 65)", benchA, benchA);
        jdbc.update("INSERT INTO routine_set (routine_exercise_id, set_number, target_reps, target_weight_kg) VALUES (?, 1, 5, 100)", squatB);
    }

    @Test
    void fullSessionFlowKeepsTargetsAndActualsSeparate() throws Exception {
        mvc.perform(get("/api/sessions/next").header("X-User-Id", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routineWorkoutId").value(workoutA))
                .andExpect(jsonPath("$.inProgressSessionId").doesNotExist());

        String started = mvc.perform(post("/api/sessions/start").header("X-User-Id", clientId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workoutName").value("Jour A"))
                .andExpect(jsonPath("$.exercises[0].coachTip").value("Omoplates serrées"))
                .andExpect(jsonPath("$.exercises[0].sets[0].actualReps").value(10))
                .andReturn().getResponse().getContentAsString();
        long sessionId = Long.parseLong(started.replaceAll("^\\{\"id\":(\\d+).*", "$1"));
        long firstSetId = jdbc.queryForObject("""
                SELECT ss.id FROM session_set ss JOIN session_exercise se ON se.id = ss.session_exercise_id
                WHERE se.session_id = ? AND ss.set_number = 1""", Long.class, sessionId);

        mvc.perform(post("/api/sessions/start").header("X-User-Id", clientId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_ALREADY_IN_PROGRESS"));

        mvc.perform(put("/api/sessions/{id}/sets/{setId}", sessionId, firstSetId)
                        .header("X-User-Id", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualReps\": 9, \"actualWeightKg\": 62.5, \"completed\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetReps").value(10))
                .andExpect(jsonPath("$.actualReps").value(9))
                .andExpect(jsonPath("$.completed").value(true));

        mvc.perform(get("/api/sessions/{id}", sessionId).header("X-User-Id", otherClientId))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/sessions/{id}/complete", sessionId).header("X-User-Id", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalSets").value(2))
                .andExpect(jsonPath("$.completedSets").value(1));

        mvc.perform(put("/api/sessions/{id}/sets/{setId}", sessionId, firstSetId)
                        .header("X-User-Id", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualReps\": 10, \"completed\": true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_IN_PROGRESS"));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT target_reps, actual_reps, target_weight_kg, actual_weight_kg FROM session_set WHERE id = ?", firstSetId);
        assertThat(((Number) row.get("target_reps")).intValue()).isEqualTo(10);
        assertThat(((Number) row.get("actual_reps")).intValue()).isEqualTo(9);
        assertThat(row.get("target_weight_kg").toString()).isEqualTo("60.00");
        assertThat(row.get("actual_weight_kg").toString()).isEqualTo("62.50");

        mvc.perform(get("/api/sessions/next").header("X-User-Id", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routineWorkoutId").value(workoutB));
    }

    private long insert(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }
}
