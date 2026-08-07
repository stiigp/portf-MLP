package com.panucci.mlp.services;

import java.util.concurrent.Executor;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.panucci.mlp.core.datastructures.MLP;
import com.panucci.mlp.core.util.ActivationFunction;
import com.panucci.mlp.dto.StartTrainingPayload;
import com.panucci.mlp.dto.TrainingEvent;
import com.panucci.mlp.listeners.TrainingListener;
import com.panucci.mlp.services.factories.MlpFactory;
import com.panucci.mlp.services.factories.ReaderFactory;
import com.panucci.mlp.core.dataprocessing.Reader;

@Service
public class MlpTrainingService {

    private final SimpMessagingTemplate messagingTemplate;
    private final Executor trainingExecutor;
    private final ReaderFactory readerFactory;
    private final MlpFactory mlpFactory;

    public MlpTrainingService(SimpMessagingTemplate messagingTemplate, @Qualifier("trainingExecutor") Executor trainingExecutor, @Qualifier("defaultReaderFactory") ReaderFactory readerFactory, @Qualifier("defaultMlpFactory") MlpFactory mlpFactory) {
        this.messagingTemplate = messagingTemplate;
        this.trainingExecutor = trainingExecutor;
        this.readerFactory = readerFactory;
        this.mlpFactory = mlpFactory;
    }

    public void startTraining(StartTrainingPayload payload) {
        this.trainingExecutor.execute(() -> {
            this.runTraining(payload);
        });
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
            payload.sessionId()
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

    private void publish(TrainingEvent event) {
        this.messagingTemplate.convertAndSend(
                "/topic/mlp/" + event.sessionId() + "/status",
                event
        );
    }

    private TrainingListener defaultTrainingListener() {
        return new TrainingListener() {
            @Override
            public void onTrainingStartEvent(TrainingEvent event) {
                publish(event);
            }

            @Override
            public void onForwardPassEvent(TrainingEvent event) {
                publish(event);
            }

            @Override
            public void onWeightsUpdateEvent(TrainingEvent event) {
                publish(event);
            }

            @Override
            public void onTrainingEndEvent(TrainingEvent event) {
                publish(event);
            }
        };
    }

}
