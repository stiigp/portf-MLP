package com.panucci.mlp.dto;

public record StartTrainingPayload(
    String sessionId,
    String databaseName,
    int hiddenLayersNumber,
    String activationFunctionName,
    double learningRate,
    double stopError,
    int maxEpochs,
    TrainingEventOptions eventOptions
) {
    public StartTrainingPayload withSessionId(String sessionId) {
        return new StartTrainingPayload(
            sessionId,
            this.databaseName,
            this.hiddenLayersNumber,
            this.activationFunctionName,
            this.learningRate,
            this.stopError,
            this.maxEpochs,
            this.eventOptions
        );
    }
}
