package com.panucci.mlp.controller;

import com.panucci.mlp.services.MlpTrainingService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class MlpSocketController {

    private final MlpTrainingService trainingService;

    public MlpSocketController(MlpTrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @MessageMapping("/mlp/{sessionId}/start")
    public void start(@DestinationVariable String sessionId) {
        System.out.println(
                "starting training on session " + sessionId
        );
    }

    @MessageMapping("/mlp/{sessionId}/pause")
    public void pause(@DestinationVariable String sessionId) {
        System.out.println(
                "pausing yet to be implemented, sessionId: " + sessionId
        );
    }
}
