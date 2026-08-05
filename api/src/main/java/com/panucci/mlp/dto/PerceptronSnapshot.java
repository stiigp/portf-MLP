package com.panucci.mlp.dto;

import java.util.List;

public record PerceptronSnapshot(
    String id,
    Double net,
    Double output,
    Double error,
    Double gradient,
    List<InputSnapshot> inputs
) {}
