package com.panucci.mlp.dto;

import java.util.List;

public record TestStartedEvent(
    String type,
    String testSessionId,
    String trainingSessionId,
    int totalSamples,
    List<String> classLabels
) implements TestMessage {
}