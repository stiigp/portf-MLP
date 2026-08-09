package com.panucci.mlp.services;

import java.util.concurrent.Executor;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;

import com.panucci.mlp.core.datastructures.MLP;
import com.panucci.mlp.core.util.ActivationFunction;
import com.panucci.mlp.dto.StartTrainingPayload;
import com.panucci.mlp.dto.TrainingEventOptions;
import com.panucci.mlp.dto.TrainingMessage;
import com.panucci.mlp.listeners.TrainingListener;
import com.panucci.mlp.services.factories.MlpFactory;
import com.panucci.mlp.services.factories.ReaderFactory;
import com.panucci.mlp.services.publishing.TrainingEventPublisher;
import com.panucci.mlp.core.dataprocessing.Reader;

@Service
public class MlpTrainingService {

    private final TrainingEventPublisher eventPublisher;
    private final Executor trainingExecutor;
    private final ReaderFactory readerFactory;
    private final MlpFactory mlpFactory;

    public MlpTrainingService(@Qualifier("webSocketTrainingEventPublisher") TrainingEventPublisher eventPublisher, @Qualifier("trainingExecutor") Executor trainingExecutor, @Qualifier("defaultReaderFactory") ReaderFactory readerFactory, @Qualifier("defaultMlpFactory") MlpFactory mlpFactory) {
        this.eventPublisher = eventPublisher;
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
