package com.panucci.mlp.dto;

import java.util.List;

public record TrainingStartedEvent(
    String type,
    String sessionId,
    int epoch,
    int sampleIndex,
    List<LayerTopology> layers
) implements TrainingMessage {}
