package com.panucci.mlp.services;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;

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
    private final AsyncTaskExecutor trainingExecutor;
    private final ReaderFactory readerFactory;
    private final MlpFactory mlpFactory;
    private final TrainingSessionService trainingSessionService;

    public MlpTrainingService(
        @Qualifier("webSocketTrainingEventPublisher") TrainingEventPublisher eventPublisher,
        @Qualifier("trainingExecutor") AsyncTaskExecutor trainingExecutor,
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
        TrainingSession session = this.trainingSessionService.createQueuedSession(payload.sessionId());
        StartTrainingPayload sessionPayload = payload.withSessionId(session.sessionId());

        try {
            this.trainingSessionService.attachTask(
                session.sessionId(),
                this.trainingExecutor.submit(() -> {
                    this.trainingSessionService.markRunning(session.sessionId());

                    try {
                        this.runTraining(sessionPayload);
                        this.trainingSessionService.markFinished(session.sessionId());
                    } catch (Exception exception) {
                        this.trainingSessionService.markFailed(session.sessionId(), exception.getMessage());
                    }
                })
            );
        } catch (TaskRejectedException exception) {
            this.trainingSessionService.markRejected(session.sessionId(), "Training queue is full");
        }

        return session;
    }

    private void runTraining(StartTrainingPayload payload) {
        ActivationFunction resolvedActivationFunction = this.resolveActivationFunction(payload.activationFunctionName());
        if (resolvedActivationFunction == null) {
            // raise error?
            return;
        }

        TrainingListener listener = this.defaultTrainingListener();

        String targetClassName = Reader.tableNameToTargetClass.getOrDefault(payload.databaseName(), null);
        if (targetClassName == null) {
            // raise error?
            return;
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
