package com.panucci.mlp.dto;

public record CreateTestSessionResponse(
    String testSessionId,
    String trainingSessionId
) {
}