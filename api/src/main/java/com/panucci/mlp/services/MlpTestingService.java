package com.panucci.mlp.services;

import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.panucci.mlp.core.datastructures.MLP;
import com.panucci.mlp.dto.TestMessage;
import com.panucci.mlp.listeners.TestListener;
import com.panucci.mlp.services.publishing.TestEventPublisher;
import com.panucci.mlp.services.sessions.TestDataset;
import com.panucci.mlp.services.sessions.TestSession;
import com.panucci.mlp.services.sessions.TestSessionService;
import com.panucci.mlp.services.sessions.TrainingSession;
import com.panucci.mlp.services.sessions.TrainingSessionService;
import com.panucci.mlp.services.sessions.TrainingSessionStatus;

@Service
public class MlpTestingService {

    private final TestSessionService testSessionService;
    private final TrainingSessionService trainingSessionService;
    private final TestEventPublisher eventPublisher;

    public MlpTestingService(
        TestSessionService testSessionService,
        TrainingSessionService trainingSessionService,
        @Qualifier("webSocketTestEventPublisher") TestEventPublisher eventPublisher
    ) {
        this.testSessionService = testSessionService;
        this.trainingSessionService = trainingSessionService;
        this.eventPublisher = eventPublisher;
    }

    public double startTesting(String testSessionId) {
        TestSession testSession = this.testSessionService.findById(testSessionId)
            .orElseThrow(() -> new NoSuchElementException("Test session does not exist"));
        TrainingSession trainingSession = this.trainingSessionService
            .findById(testSession.trainingSessionId())
            .orElseThrow(() -> new NoSuchElementException("Training session does not exist"));

        if (trainingSession.status() != TrainingSessionStatus.FINISHED) {
            throw new IllegalStateException("Training session has not finished");
        }

        MLP trainedModel = trainingSession.trainedModel();
        TestDataset testDataset = trainingSession.testDataset();
        if (trainedModel == null || testDataset == null) {
            throw new IllegalStateException("Training session is not ready for testing");
        }

        trainedModel.setTestListener(this.defaultTestListener());

        return trainedModel.test(
            testDataset.testTable(),
            testDataset.targetClassName(),
            testSession.testSessionId()
        );
    }

    private void publish(TestMessage event) {
        this.eventPublisher.publish(event);
    }

    private TestListener defaultTestListener() {
        return new TestListener() {
            @Override
            public void onTestStartEvent(TestMessage event) {
                publish(event);
            }

            @Override
            public void onTestProgressEvent(TestMessage event) {
                publish(event);
            }

            @Override
            public void onTestEndEvent(TestMessage event) {
                publish(event);
            }
        };
    }
}