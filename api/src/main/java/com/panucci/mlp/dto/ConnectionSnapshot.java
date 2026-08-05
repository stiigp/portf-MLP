package com.panucci.mlp.dto;

public record ConnectionSnapshot(
    String from,
    String to,
    double weight
) {
    
}
