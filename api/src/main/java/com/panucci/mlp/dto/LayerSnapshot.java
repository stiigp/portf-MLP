package com.panucci.mlp.dto;

import java.util.List;

public record LayerSnapshot(
    String type,
    int index,
    List<PerceptronSnapshot> perceptrons
) {}
