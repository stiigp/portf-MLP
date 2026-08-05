package com.panucci.mlp.dto;

public record InputSnapshot(
    String fromNeuronId,
    double weight,
    double inputOutput
) {}
