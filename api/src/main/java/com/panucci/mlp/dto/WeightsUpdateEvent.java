package com.panucci.mlp.dto;

import java.util.List;

public record WeightsUpdateEvent(
    String type,
    String sessionId,
    int epoch,
    int sampleIndex,
    List<ConnectionSnapshot> connections
) implements TrainingMessage {}
