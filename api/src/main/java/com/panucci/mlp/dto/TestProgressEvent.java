package com.panucci.mlp.dto;

public record TestProgressEvent(
    String type,
    String testSessionId,
    int sampleIndex,
    int predictedClassIndex,
    int expectedClassIndex,
    double accuracy,
    double precision,
    double recall,
    double f1Score
) implements TestMessage {
    public TestProgressEvent(
        String type,
        String testSessionId,
        int sampleIndex,
        int predictedClassIndex,
        int expectedClassIndex,
        double accuracy,
        double precision,
        double f1Score
    ) {
        this(
            type,
            testSessionId,
            sampleIndex,
            predictedClassIndex,
            expectedClassIndex,
            accuracy,
            precision,
            0.0,
            f1Score
        );
    }
}
