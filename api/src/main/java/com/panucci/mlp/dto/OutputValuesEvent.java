package com.panucci.mlp.dto;

import java.util.List;

public record OutputValuesEvent(
    String type,
    String sessionId,
    int epoch,
    int sampleIndex,
    List<OutputValueSnapshot> outputs
) implements TrainingMessage {}
