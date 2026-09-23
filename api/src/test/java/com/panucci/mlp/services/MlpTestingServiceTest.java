package com.panucci.mlp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

import com.panucci.mlp.core.datastructures.MLP;
import com.panucci.mlp.dto.TestProgressEvent;
import com.panucci.mlp.listeners.TestListener;
import com.panucci.mlp.services.publishing.TestEventPublisher;
import com.panucci.mlp.services.sessions.TestDataset;
import com.panucci.mlp.services.sessions.TestSession;
import com.panucci.mlp.services.sessions.TestSessionService;
import com.panucci.mlp.services.sessions.TrainingSession;
import com.panucci.mlp.services.sessions.TrainingSessionService;
import com.panucci.mlp.services.sessions.TrainingSessionStatus;

import tech.tablesaw.api.Table;

class MlpTestingServiceTest {

    private TestSessionService testSessionService;
    private TrainingSessionService trainingSessionService;
    private TestEventPublisher eventPublisher;
    private MlpTestingService service;

    @BeforeEach
    void setUp() {
        this.testSessionService = mock(TestSessionService.class);
        this.trainingSessionService = mock(TrainingSessionService.class);
        this.eventPublisher = mock(TestEventPublisher.class);
        this.service = new MlpTestingService(
            this.testSessionService,
            this.trainingSessionService,
            this.eventPublisher
        );
    }

    @Test
    void testsTheReservedDatasetOfTheTrainingSession() {
        MLP trainedModel = mock(MLP.class);
        Table testTable = Table.create("test");
        TestSession testSession = new TestSession(
            "test-session-1",
            "training-session-1",
            Instant.now(),
            Instant.now().plusSeconds(60)
        );
        TrainingSession trainingSession = trainingSession(
            trainedModel,
            new TestDataset("iris", "Species", testTable)
        );
        when(this.testSessionService.findById("test-session-1"))
            .thenReturn(Optional.of(testSession));
        when(this.trainingSessionService.findById("training-session-1"))
            .thenReturn(Optional.of(trainingSession));
        when(trainedModel.test(testTable, "Species", "test-session-1")).thenReturn(0.92);

        double accuracy = this.service.startTesting("test-session-1");

        assertEquals(0.92, accuracy);
        ArgumentCaptor<TestListener> listenerCaptor = ArgumentCaptor.forClass(TestListener.class);
        verify(trainedModel).setTestListener(listenerCaptor.capture());
        verify(trainedModel).test(testTable, "Species", "test-session-1");

        TestProgressEvent event = new TestProgressEvent(
            "TEST_PROGRESS",
            "test-session-1",
            0,
            1,
            1,
            1.0,
            1.0,
            1.0
        );
        listenerCaptor.getValue().onTestProgressEvent(event);

        verify(this.eventPublisher).publish(event);
    }

    @Test
    void rejectsUnknownTestSessions() {
        when(this.testSessionService.findById("missing"))
            .thenReturn(Optional.empty());

        assertThrows(
            NoSuchElementException.class,
            () -> this.service.startTesting("missing")
        );
    }

    private TrainingSession trainingSession(MLP trainedModel, TestDataset testDataset) {
        Instant now = Instant.now();
        return new TrainingSession(
            "training-session-1",
            TrainingSessionStatus.FINISHED,
            now,
            now,
            now,
            now.plusSeconds(60),
            null,
            null,
            trainedModel,
            testDataset
        );
    }
}