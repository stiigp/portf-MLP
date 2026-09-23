package com.panucci.mlp.dto;

public record TestProgressEvent(
    String type,
    String testSessionId,
    int sampleIndex,
    int predictedClassIndex,
    int expectedClassIndex,
    double accuracy,
    double precision,
    double f1Score
) implements TestMessage {
}