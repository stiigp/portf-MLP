package com.panucci.mlp.dto;

import com.panucci.mlp.services.sessions.TrainingSessionStatus;

public record CreateTrainingSessionResponse(
    String sessionId,
    TrainingSessionStatus status
) {
}
