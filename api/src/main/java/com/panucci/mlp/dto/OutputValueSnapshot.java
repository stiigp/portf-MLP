package com.panucci.mlp.dto;

public record OutputValueSnapshot(
    String id,
    Double net,
    Double output,
    int expected
) {}
