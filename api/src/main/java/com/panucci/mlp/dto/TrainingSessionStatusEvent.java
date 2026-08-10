package com.panucci.mlp.dto;

import com.panucci.mlp.services.sessions.TrainingSessionStatus;

public record TrainingSessionStatusEvent(
    String type,
    String sessionId,
    TrainingSessionStatus status,
    String failureReason
) implements TrainingMessage {
}
