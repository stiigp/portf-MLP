package com.panucci.mlp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.panucci.mlp.core.datastructures.MLP;
import com.panucci.mlp.dto.CreateTestSessionRequest;
import com.panucci.mlp.dto.CreateTestSessionResponse;
import com.panucci.mlp.services.sessions.TestSession;
import com.panucci.mlp.services.sessions.TestSessionService;
import com.panucci.mlp.services.sessions.TrainingSession;
import com.panucci.mlp.services.sessions.TrainingSessionService;
import com.panucci.mlp.services.sessions.TrainingSessionStatus;

class MlpSessionControllerTest {

    private TrainingSessionService trainingSessionService;
    private TestSessionService testSessionService;
    private MlpSessionController controller;

    @BeforeEach
    void setUp() {
        this.trainingSessionService = mock(TrainingSessionService.class);
        this.testSessionService = mock(TestSessionService.class);
        this.controller = new MlpSessionController(
            this.trainingSessionService,
            this.testSessionService
        );
    }

    @Test
    void createsTestSessionForFinishedTrainingWithModel() {
        TrainingSession trainingSession = trainingSession(
            TrainingSessionStatus.FINISHED,
            mock(MLP.class)
        );
        TestSession testSession = new TestSession(
            "test-session-1",
            "training-session-1",
            Instant.now(),
            Instant.now().plusSeconds(60)
        );
        when(this.trainingSessionService.findById("training-session-1"))
            .thenReturn(Optional.of(trainingSession));
        when(this.testSessionService.createSession("training-session-1"))
            .thenReturn(testSession);

        CreateTestSessionResponse response = this.controller.createTestSession(
            new CreateTestSessionRequest("training-session-1")
        );

        assertEquals("test-session-1", response.testSessionId());
        assertEquals("training-session-1", response.trainingSessionId());
    }

    @Test
    void rejectsTestSessionWhenTrainingDoesNotExist() {
        when(this.trainingSessionService.findById("missing"))
            .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> this.controller.createTestSession(new CreateTestSessionRequest("missing"))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void rejectsTestSessionWhenTrainingHasNotFinished() {
        when(this.trainingSessionService.findById("training-session-1"))
            .thenReturn(Optional.of(trainingSession(TrainingSessionStatus.RUNNING, mock(MLP.class))));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> this.controller.createTestSession(
                new CreateTestSessionRequest("training-session-1")
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void rejectsTestSessionWhenTrainingHasNoModel() {
        when(this.trainingSessionService.findById("training-session-1"))
            .thenReturn(Optional.of(trainingSession(TrainingSessionStatus.FINISHED, null)));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> this.controller.createTestSession(
                new CreateTestSessionRequest("training-session-1")
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    private TrainingSession trainingSession(TrainingSessionStatus status, MLP trainedModel) {
        Instant now = Instant.now();
        return new TrainingSession(
            "training-session-1",
            status,
            now,
            now,
            now,
            now.plusSeconds(60),
            null,
            null,
            trainedModel
        );
    }
}