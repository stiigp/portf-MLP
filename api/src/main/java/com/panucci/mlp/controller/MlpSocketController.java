package com.panucci.mlp.controller;

import com.panucci.mlp.dto.StartTrainingPayload;
import com.panucci.mlp.services.MlpTestingService;
import com.panucci.mlp.services.MlpTrainingService;
import com.panucci.mlp.services.sessions.TrainingSession;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Controller
public class MlpSocketController {

    private final MlpTrainingService trainingService;
    private final MlpTestingService testingService;
    private final ConcurrentHashMap<String, String> simpSessionToTrainingSessionHashMap = new ConcurrentHashMap<>();

    public MlpSocketController(
        MlpTrainingService trainingService,
        MlpTestingService testingService
    ) {
        this.trainingService = trainingService;
        this.testingService = testingService;
    }

    @MessageMapping("/mlp/start")
    public void start(
        @Payload StartTrainingPayload payload,
        SimpMessageHeaderAccessor headers
    ) {
        String simpSessionId = headers.getSessionId();

        TrainingSession session = this.trainingService.startTraining(payload);

        this.simpSessionToTrainingSessionHashMap.put(simpSessionId, session.sessionId());

        System.out.println(
                "starting training on session " + session.sessionId()
        );
    }

    @MessageMapping("/mlp/tests/{testSessionId}/start")
    public void startTest(@DestinationVariable String testSessionId) {
        this.testingService.startTesting(testSessionId);
    }

    @MessageMapping("/mlp/{sessionId}/pause")
    public void pause(@DestinationVariable String sessionId) {
        System.out.println(
                "pausing yet to be implemented, sessionId: " + sessionId
        );
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String simpSessionId = event.getSessionId();

        String trainingSessionId = this.simpSessionToTrainingSessionHashMap.remove(simpSessionId);
        if (trainingSessionId != null) {
            this.trainingService.cancelTraining(trainingSessionId);
        }
    }
}
