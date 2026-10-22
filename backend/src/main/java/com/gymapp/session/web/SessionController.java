package com.gymapp.session.web;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gymapp.security.CurrentUserProvider;
import com.gymapp.session.service.SessionService;
import com.gymapp.session.web.dto.NextWorkoutResponse;
import com.gymapp.session.web.dto.SessionResponse;
import com.gymapp.session.web.dto.SessionSetResponse;
import com.gymapp.session.web.dto.SessionSummaryResponse;
import com.gymapp.session.web.dto.StartSessionRequest;
import com.gymapp.session.web.dto.UpdateSetRequest;

/** Exécution d'une séance, côté client. */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;
    private final CurrentUserProvider currentUser;

    public SessionController(SessionService sessionService, CurrentUserProvider currentUser) {
        this.sessionService = sessionService;
        this.currentUser = currentUser;
    }

    /** Dashboard : prochaine séance de la rotation (et séance en cours éventuelle). */
    @GetMapping("/next")
    public NextWorkoutResponse next() {
        return sessionService.getNextWorkout(currentUser.requireClientId());
    }

    /** Lance la prochaine séance, ou celle demandée dans le corps. */
    @PostMapping("/start")
    public ResponseEntity<SessionResponse> start(@Valid @RequestBody(required = false) StartSessionRequest request) {
        SessionResponse session = sessionService.start(currentUser.requireClientId(), request);
        return ResponseEntity.created(URI.create("/api/sessions/" + session.id())).body(session);
    }

    /** Séance en cours (200) ou aucune (204). */
    @GetMapping("/current")
    public ResponseEntity<SessionResponse> current() {
        return sessionService.findCurrent(currentUser.requireClientId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{sessionId}")
    public SessionResponse get(@PathVariable Long sessionId) {
        return sessionService.get(currentUser.requireClientId(), sessionId);
    }

    /** Mise à jour d'une série en temps réel (reps/poids réels, case cochée). */
    @PutMapping("/{sessionId}/sets/{setId}")
    public SessionSetResponse updateSet(@PathVariable Long sessionId,
                                        @PathVariable Long setId,
                                        @Valid @RequestBody UpdateSetRequest request) {
        return sessionService.updateSet(currentUser.requireClientId(), sessionId, setId, request);
    }

    @PostMapping("/{sessionId}/complete")
    public SessionSummaryResponse complete(@PathVariable Long sessionId) {
        return sessionService.complete(currentUser.requireClientId(), sessionId);
    }
}
