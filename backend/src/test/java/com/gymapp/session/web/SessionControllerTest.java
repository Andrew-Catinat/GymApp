package com.gymapp.session.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gymapp.common.exception.BusinessException;
import com.gymapp.common.exception.ErrorCode;
import com.gymapp.common.exception.ResourceNotFoundException;
import com.gymapp.security.CurrentUserProvider;
import com.gymapp.session.domain.SessionStatus;
import com.gymapp.session.service.SessionService;
import com.gymapp.session.web.dto.SessionResponse;
import com.gymapp.session.web.dto.SessionSetResponse;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    private static final long CLIENT_ID = 2L;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SessionService sessionService;

    @MockitoBean
    private CurrentUserProvider currentUser;

    @BeforeEach
    void authenticateAsClient() {
        given(currentUser.requireClientId()).willReturn(CLIENT_ID);
    }

    @Test
    void startReturns201WithLocation() throws Exception {
        given(sessionService.start(eq(CLIENT_ID), any())).willReturn(new SessionResponse(
                7L, "Jour A", SessionStatus.IN_PROGRESS, Instant.parse("2026-10-22T10:00:00Z"), null, 0, 0, List.of()));

        mvc.perform(post("/api/sessions/start"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/sessions/7"))
                .andExpect(jsonPath("$.workoutName").value("Jour A"));
    }

    @Test
    void startWhileASessionIsInProgressReturns409WithItsId() throws Exception {
        given(sessionService.start(eq(CLIENT_ID), any())).willThrow(new BusinessException(
                ErrorCode.SESSION_ALREADY_IN_PROGRESS, "Une séance est déjà en cours.", Map.of("sessionId", 7L)));

        mvc.perform(post("/api/sessions/start"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_ALREADY_IN_PROGRESS"))
                .andExpect(jsonPath("$.sessionId").value(7));
    }

    @Test
    void updateSetReturnsTheUpdatedSet() throws Exception {
        given(sessionService.updateSet(eq(CLIENT_ID), eq(7L), eq(3L), any())).willReturn(new SessionSetResponse(
                3L, 1, 10, new BigDecimal("60.00"), 9, new BigDecimal("60.00"), true, Instant.now()));

        mvc.perform(put("/api/sessions/7/sets/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actualReps": 9, "actualWeightKg": 60, "completed": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualReps").value(9))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void updateSetOnUnknownSetReturns404() throws Exception {
        given(sessionService.updateSet(eq(CLIENT_ID), eq(7L), eq(99L), any()))
                .willThrow(new ResourceNotFoundException("Série", 99L));

        mvc.perform(put("/api/sessions/7/sets/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actualReps": 9, "completed": true}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.resource").value("Série"));
    }

    @Test
    void updateSetWithInvalidBodyReturns400WithoutCallingTheService() throws Exception {
        mvc.perform(put("/api/sessions/7/sets/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actualReps": -1, "actualWeightKg": 60.123}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(3));

        verifyNoInteractions(sessionService);
    }

    @Test
    void completingAFinishedSessionReturns409() throws Exception {
        given(sessionService.complete(CLIENT_ID, 7L)).willThrow(new BusinessException(
                ErrorCode.SESSION_NOT_IN_PROGRESS, "La séance est déjà terminée."));

        mvc.perform(post("/api/sessions/7/complete"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_IN_PROGRESS"));
    }

    @Test
    void currentReturns204WhenNoSessionIsInProgress() throws Exception {
        given(sessionService.findCurrent(CLIENT_ID)).willReturn(Optional.empty());

        mvc.perform(get("/api/sessions/current"))
                .andExpect(status().isNoContent());
    }

    @Test
    void nonClientUserGets403() throws Exception {
        given(currentUser.requireClientId()).willThrow(new BusinessException(
                ErrorCode.CLIENT_ROLE_REQUIRED, "Cette action est réservée aux clients."));

        mvc.perform(get("/api/sessions/next"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CLIENT_ROLE_REQUIRED"));
    }
}
