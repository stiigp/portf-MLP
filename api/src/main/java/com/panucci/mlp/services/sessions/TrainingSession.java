package com.panucci.mlp.services.sessions;

import java.time.Instant;

import com.panucci.mlp.core.datastructures.MLP;

public record TrainingSession(
    String sessionId,
    TrainingSessionStatus status,
    Instant createdAt,
    Instant startedAt,
    Instant updatedAt,
    Instant expiresAt,
    String failureReason,
    Integer queuePosition,
    MLP trainedModel
) {
    public TrainingSession(
        String sessionId,
        TrainingSessionStatus status,
        Instant createdAt,
        Instant startedAt,
        Instant updatedAt,
        Instant expiresAt,
        String failureReason,
        Integer queuePosition
    ) {
        this(sessionId, status, createdAt, startedAt, updatedAt, expiresAt, failureReason, queuePosition, null);
    }
}
