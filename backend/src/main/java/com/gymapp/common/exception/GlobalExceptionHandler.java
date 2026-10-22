package com.gymapp.common.exception;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduit les exceptions en réponses RFC 9457 (application/problem+json).
 * Chaque erreur porte un champ {@code code} stable que le frontend peut tester.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Ressource introuvable");
        problem.setProperty("code", "RESOURCE_NOT_FOUND");
        problem.setProperty("resource", ex.getResource());
        problem.setProperty("resourceId", ex.getResourceId());
        return problem;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getCode().status(), ex.getMessage());
        problem.setTitle(ex.getCode().title());
        problem.setProperty("code", ex.getCode().name());
        ex.getDetails().forEach(problem::setProperty);
        return problem;
    }

    /** Filet de sécurité : contrainte SQL violée (ex. deux séances lancées en même temps). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Contrainte d'intégrité violée", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "L'opération entre en conflit avec l'état actuel des données.");
        problem.setTitle("Conflit");
        problem.setProperty("code", "DATA_CONFLICT");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erreur inattendue", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur inattendue est survenue.");
        problem.setTitle("Erreur interne");
        problem.setProperty("code", "INTERNAL_ERROR");
        return problem;
    }

    /** Erreurs de validation des DTO (@Valid) : 400 avec la liste des champs en erreur. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "La requête contient des champs invalides.");
        problem.setTitle("Requête invalide");
        problem.setProperty("code", "VALIDATION_FAILED");
        List<Map<String, Object>> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> Map.<String, Object>of(
                        "field", error instanceof FieldError fe ? fe.getField() : error.getObjectName(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }
}
