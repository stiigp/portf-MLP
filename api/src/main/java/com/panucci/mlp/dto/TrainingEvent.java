package com.panucci.mlp.dto;

import java.util.List;

public record TrainingEvent(
    String type,
    String sessionId,
    int epoch,
    int sampleIndex,
    double networkError,
    LayerSnapshot inputLayer,
    List<LayerSnapshot> hiddenLayers,
    LayerSnapshot outputLayer
) {}
