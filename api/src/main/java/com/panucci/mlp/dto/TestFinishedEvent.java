package com.panucci.mlp.dto;

public record TestFinishedEvent(
    String type,
    String testSessionId,
    int processedSamples,
    int correctPredictions,
    double accuracy,
    double precision,
    double f1Score
) implements TestMessage {
}