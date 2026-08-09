package com.panucci.mlp.dto;

import java.util.List;

public record LayerTopology(
    String type,
    int index,
    List<String> perceptronIds
) {}
