package com.panucci.mlp.controller;

import com.panucci.mlp.dto.CreateTrainingSessionResponse;
import com.panucci.mlp.services.sessions.TrainingSession;
import com.panucci.mlp.services.sessions.TrainingSessionService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mlp/sessions")
public class MlpSessionController {

    private final TrainingSessionService trainingSessionService;

    public MlpSessionController(TrainingSessionService trainingSessionService) {
        this.trainingSessionService = trainingSessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateTrainingSessionResponse create() {
        TrainingSession session = this.trainingSessionService.createSession();

        return new CreateTrainingSessionResponse(
            session.sessionId(),
            session.status()
        );
    }
}
