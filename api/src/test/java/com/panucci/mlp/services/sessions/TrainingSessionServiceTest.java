package com.panucci.mlp.services.sessions;

import com.panucci.mlp.dto.TrainingSessionStatusEvent;
import com.panucci.mlp.services.publishing.TrainingEventPublisher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrainingSessionServiceTest {

    @Test
    void refreshQueuedSessionsPositionsPublishesPositionPerQueuedSession() {
        TrainingEventPublisher eventPublisher = mock(TrainingEventPublisher.class);
        TrainingSessionService service = new TrainingSessionService(
            new SequenceSessionIdGenerator("session-1", "session-2"),
            eventPublisher,
            120000,
            600000,
            600000,
            50
        );
        Future<?> firstTask = mock(Future.class);
        Future<?> secondTask = mock(Future.class);

        when(firstTask.isDone()).thenReturn(false);
        when(secondTask.isDone()).thenReturn(false);

        service.createSession();
        service.createSession();
        service.markQueued("session-1");
        service.markQueued("session-2");
        service.attachTask("session-1", firstTask);
        service.attachTask("session-2", secondTask);

        service.refreshQueuedSessionsPositions(task -> task == firstTask ? 2 : 1);

        assertEquals(2, service.findById("session-1").orElseThrow().queuePosition());
        assertEquals(1, service.findById("session-2").orElseThrow().queuePosition());
        verify(eventPublisher).publish(new TrainingSessionStatusEvent(
            "SESSION_STATUS",
            "session-1",
            TrainingSessionStatus.QUEUED,
            null,
            2
        ));
        verify(eventPublisher).publish(new TrainingSessionStatusEvent(
            "SESSION_STATUS",
            "session-2",
            TrainingSessionStatus.QUEUED,
            null,
            1
        ));
    }

    private static class SequenceSessionIdGenerator implements SessionIdGenerator {

        private final List<String> sessionIds;
        private int nextSessionIndex;

        private SequenceSessionIdGenerator(String... sessionIds) {
            this.sessionIds = List.of(sessionIds);
        }

        @Override
        public String generate() {
            return this.sessionIds.get(this.nextSessionIndex++);
        }
    }
}