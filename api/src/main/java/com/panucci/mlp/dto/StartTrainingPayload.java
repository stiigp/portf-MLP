package com.panucci.mlp.dto;

public record StartTrainingPayload(
    String sessionId,
    String databaseName,
    int hiddenLayersNumber,
    String activationFunctionName,
    double learningRate,
    double stopError,
    int maxEpochs
) {
    
}
