package com.panucci.mlp.dto;

import java.util.List;

public record TestStartedEvent(
    String type,
    String testSessionId,
    String trainingSessionId,
    int totalSamples,
    List<String> classLabels,
    List<Integer> classSampleTotals
) implements TestMessage {
    public TestStartedEvent(
        String type,
        String testSessionId,
        String trainingSessionId,
        int totalSamples,
        List<String> classLabels
    ) {
        this(
            type,
            testSessionId,
            trainingSessionId,
            totalSamples,
            classLabels,
            List.of()
        );
    }
}
