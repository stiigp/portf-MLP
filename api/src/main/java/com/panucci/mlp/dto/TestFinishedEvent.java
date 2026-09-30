package com.panucci.mlp.dto;

public record TestFinishedEvent(
    String type,
    String testSessionId,
    int processedSamples,
    int correctPredictions,
    double accuracy,
    double precision,
    double recall,
    double f1Score
) implements TestMessage {
    public TestFinishedEvent(
        String type,
        String testSessionId,
        int processedSamples,
        int correctPredictions,
        double accuracy,
        double precision,
        double f1Score
    ) {
        this(
            type,
            testSessionId,
            processedSamples,
            correctPredictions,
            accuracy,
            precision,
            0.0,
            f1Score
        );
    }
}
