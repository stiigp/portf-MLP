package com.panucci.mlp.dto;

public record InputSnapshot(
    String fromPerceptronId,
    double weight,
    double inputOutput
) {}
