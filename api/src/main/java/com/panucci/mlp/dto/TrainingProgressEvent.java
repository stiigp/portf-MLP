package com.panucci.mlp.dto;

public record TrainingProgressEvent(
    String type,
    String sessionId,
    int epoch,
    int sampleIndex,
    double networkError
) implements TrainingMessage {}
