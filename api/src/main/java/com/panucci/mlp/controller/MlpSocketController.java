package com.panucci.mlp.controller;

import com.panucci.mlp.dto.StartTrainingPayload;
import com.panucci.mlp.services.MlpTrainingService;
import com.panucci.mlp.services.sessions.TrainingSession;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class MlpSocketController {

    private final MlpTrainingService trainingService;
 
    public MlpSocketController(MlpTrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @MessageMapping("/mlp/start")
    public void start(@Payload StartTrainingPayload payload) {

        TrainingSession session = this.trainingService.startTraining(payload);

        System.out.println(
                "starting training on session " + session.sessionId()
        );
    }

    @MessageMapping("/mlp/{sessionId}/pause")                   
    public void pause(@DestinationVariable String sessionId) {
        System.out.println(
                "pausing yet to be implemented, sessionId: " + sessionId
        );
    }
}
