package com.gymapp.common.exception;

import org.springframework.http.HttpStatus;

/** Codes d'erreur métier stables, exploitables par le frontend. */
public enum ErrorCode {
    NO_ACTIVE_ROUTINE(HttpStatus.CONFLICT),
    ROUTINE_HAS_NO_WORKOUT(HttpStatus.CONFLICT),
    WORKOUT_NOT_IN_ACTIVE_ROUTINE(HttpStatus.CONFLICT),
    WORKOUT_HAS_NO_EXERCISE(HttpStatus.CONFLICT),
    SESSION_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT),
    SESSION_NOT_IN_PROGRESS(HttpStatus.CONFLICT),
    CLIENT_ROLE_REQUIRED(HttpStatus.FORBIDDEN),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return switch (status) {
            case UNAUTHORIZED -> "Authentification requise";
            case FORBIDDEN -> "Accès refusé";
            default -> "Règle métier non respectée";
        };
    }
}
