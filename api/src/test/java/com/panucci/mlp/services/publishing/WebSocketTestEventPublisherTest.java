package com.panucci.mlp.services.publishing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.panucci.mlp.dto.TestProgressEvent;

class WebSocketTestEventPublisherTest {

    @Test
    void publishesToTheTestSessionTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        WebSocketTestEventPublisher publisher = new WebSocketTestEventPublisher(messagingTemplate);
        TestProgressEvent event = new TestProgressEvent(
            "TEST_PROGRESS",
            "test-session-1",
            4,
            1,
            0,
            0.8,
            0.75,
            0.7
        );

        publisher.publish(event);

        verify(messagingTemplate).convertAndSend(
            "/topic/mlp/tests/test-session-1",
            event
        );
    }
}