package com.panucci.mlp.services;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.panucci.mlp.core.datastructures.MLP;
import com.panucci.mlp.core.util.ActivationFunction;
import com.panucci.mlp.dto.StartTrainingPayload;
import com.panucci.mlp.dto.TrainingEventOptions;
import com.panucci.mlp.dto.TrainingMessage;
import com.panucci.mlp.listeners.TrainingListener;
import com.panucci.mlp.services.factories.MlpFactory;
import com.panucci.mlp.services.factories.ReaderFactory;
import com.panucci.mlp.services.publishing.TrainingEventPublisher;
import com.panucci.mlp.services.sessions.TrainingSession;
import com.panucci.mlp.services.sessions.TrainingSessionService;
import com.panucci.mlp.core.dataprocessing.Reader;

@Service
public class MlpTrainingService {

    private final TrainingEventPublisher eventPublisher;
    private final ThreadPoolTaskExecutor trainingExecutor;
    private final ReaderFactory readerFactory;
    private final MlpFactory mlpFactory;
    private final TrainingSessionService trainingSessionService;

    public MlpTrainingService(
        @Qualifier("webSocketTrainingEventPublisher") TrainingEventPublisher eventPublisher,
        @Qualifier("trainingExecutor") ThreadPoolTaskExecutor trainingExecutor,
        @Qualifier("defaultReaderFactory") ReaderFactory readerFactory,
        @Qualifier("defaultMlpFactory") MlpFactory mlpFactory,
        TrainingSessionService trainingSessionService
    ) {
        this.eventPublisher = eventPublisher;
        this.trainingExecutor = trainingExecutor;
        this.readerFactory = readerFactory;
        this.mlpFactory = mlpFactory;
        this.trainingSessionService = trainingSessionService;
    }

    public TrainingSession startTraining(StartTrainingPayload payload) {
        TrainingSession session = this.trainingSessionService.markQueued(payload.sessionId());
        String sessionId = session.sessionId();
        StartTrainingPayload sessionPayload = payload.withSessionId(sessionId);
        CountDownLatch queuePositionPublished = new CountDownLatch(1);

        try {
            Future<?> task = this.trainingExecutor.submit(() -> {
                this.awaitQueuePositionPublication(queuePositionPublished);
                this.trainingSessionService.markRunning(sessionId);
                this.trainingSessionService.refreshQueuedSessionsPositions(this::findQueuePosition);

                try {
                    MLP trainedModel = this.runTraining(sessionPayload);
                    this.trainingSessionService.markFinished(sessionId, trainedModel);
                } catch (Exception exception) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    this.trainingSessionService.markFailed(sessionId, exception.getMessage());
                }
            });

            this.trainingSessionService.attachTask(sessionId, task);
            session = this.trainingSessionService.markQueuePosition(sessionId, this.findQueuePosition(task));
        } catch (TaskRejectedException exception) {
            this.trainingSessionService.markRejected(sessionId, "Training queue is full");
        } finally {
            queuePositionPublished.countDown();
        }

        return session;
    }

    public void cancelTraining(String sessionId) {
        boolean cancelled = this.trainingSessionService.cancelTraining(
            sessionId,
            "Training cancelled because the client disconnected"
        );

        if (cancelled) {
            this.trainingSessionService.refreshQueuedSessionsPositions(this::findQueuePosition);
        }
    }

    private void awaitQueuePositionPublication(CountDownLatch queuePositionPublished) {
        try {
            queuePositionPublished.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Training interrupted before start", exception);
        }
    }

    private Integer findQueuePosition(Future<?> task) {
        Object[] queuedTasks = this.trainingExecutor.getThreadPoolExecutor().getQueue().toArray();

        for (int index = 0; index < queuedTasks.length; index++) {
            if (queuedTasks[index] == task) {
                return index + 1;
            }
        }

        return null;        
    }

    private MLP runTraining(StartTrainingPayload payload) {
        ActivationFunction resolvedActivationFunction = this.resolveActivationFunction(payload.activationFunctionName());
        if (resolvedActivationFunction == null) {
            throw new IllegalArgumentException("Invalid activation function: " + payload.activationFunctionName());
        }

        TrainingListener listener = this.defaultTrainingListener();

        String targetClassName = Reader.tableNameToTargetClass.getOrDefault(payload.databaseName(), null);
        if (targetClassName == null) {
            throw new IllegalArgumentException("Invalid database name: " + payload.databaseName());
        }

        Reader reader = this.readerFactory.create(payload.databaseName(), targetClassName);
        reader.normaliza();
        reader.oneHotEncode();

        MLP mlp = this.mlpFactory.create(
            payload.hiddenLayersNumber(),
            resolvedActivationFunction,
            payload.learningRate(),
            listener,
            payload.sessionId(),
            TrainingEventOptions.normalize(payload.eventOptions())
        );

        mlp.train(
            reader.getTrainTable(),
            targetClassName,
            payload.stopError(),
            payload.maxEpochs()
        );

        return mlp;
    }

    private ActivationFunction resolveActivationFunction(String activationFunctionName) {
        return ActivationFunction.nameToActivationFunctionMap.getOrDefault(activationFunctionName, null);
    }

    private void publish(TrainingMessage event) {
        this.eventPublisher.publish(event);
    }

    private TrainingListener defaultTrainingListener() {
        return new TrainingListener() {
            @Override
            public void onTrainingStartEvent(TrainingMessage event) {
                publish(event);
            }

            @Override
            public void onForwardPassEvent(TrainingMessage event) {
                publish(event);
            }

            @Override
            public void onWeightsUpdateEvent(TrainingMessage event) {
                publish(event);
            }

            @Override
            public void onTrainingEndEvent(TrainingMessage event) {
                publish(event);
            }
        };
    }

}
