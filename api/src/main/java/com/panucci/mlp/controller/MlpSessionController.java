package com.panucci.mlp.controller;

import com.panucci.mlp.dto.CreateTestSessionRequest;
import com.panucci.mlp.dto.CreateTestSessionResponse;
import com.panucci.mlp.dto.CreateTrainingSessionResponse;
import com.panucci.mlp.services.sessions.TestSession;
import com.panucci.mlp.services.sessions.TestSessionService;
import com.panucci.mlp.services.sessions.TrainingSession;
import com.panucci.mlp.services.sessions.TrainingSessionStatus;
import com.panucci.mlp.services.sessions.TrainingSessionService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/mlp/sessions")
public class MlpSessionController {

    private final TrainingSessionService trainingSessionService;
    private final TestSessionService testSessionService;

    public MlpSessionController(
        TrainingSessionService trainingSessionService,
        TestSessionService testSessionService
    ) {
        this.trainingSessionService = trainingSessionService;
        this.testSessionService = testSessionService;
    }

    @PostMapping("/train")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateTrainingSessionResponse createTrainingSession() {
        TrainingSession session = this.trainingSessionService.createSession();

        return new CreateTrainingSessionResponse(
            session.sessionId(),
            session.status()
        );
    }

    @PostMapping("/test")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateTestSessionResponse createTestSession(
        @RequestBody CreateTestSessionRequest request
    ) {
        String trainingSessionId = request.trainingSessionId();
        if (trainingSessionId == null || trainingSessionId.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "trainingSessionId is required"
            );
        }

        TrainingSession trainingSession = this.trainingSessionService.findById(trainingSessionId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Training session does not exist"
            ));

        if (trainingSession.status() != TrainingSessionStatus.FINISHED) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Training session has not finished"
            );
        }

        if (trainingSession.trainedModel() == null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Training session has no trained model"
            );
        }

        TestSession testSession = this.testSessionService.createSession(trainingSessionId);
        return new CreateTestSessionResponse(
            testSession.testSessionId(),
            testSession.trainingSessionId()
        );
    }
}