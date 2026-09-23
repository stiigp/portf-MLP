package com.panucci.mlp.services.publishing;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.panucci.mlp.dto.TestMessage;

@Component("webSocketTestEventPublisher")
public class WebSocketTestEventPublisher implements TestEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketTestEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publish(TestMessage event) {
        this.messagingTemplate.convertAndSend(
            "/topic/mlp/tests/" + event.testSessionId(),
            event
        );
    }
}