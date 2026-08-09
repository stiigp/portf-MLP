package com.panucci.mlp.dto;

public record TrainingFinishedEvent(
    String type,
    String sessionId,
    int epoch,
    int sampleIndex,
    double networkError
) implements TrainingMessage {}
