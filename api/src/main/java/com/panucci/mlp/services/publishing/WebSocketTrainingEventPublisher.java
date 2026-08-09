package com.panucci.mlp.services.publishing;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.panucci.mlp.dto.TrainingMessage;

@Component("webSocketTrainingEventPublisher")
public class WebSocketTrainingEventPublisher implements TrainingEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketTrainingEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    @Override
    public void publish(TrainingMessage event) {
        this.messagingTemplate.convertAndSend(
            "/topic/mlp/" + event.sessionId() + "/status",
            event
        );
    }
}
