package com.panucci.mlp.services.sessions;

import java.time.Instant;

public record TrainingSession(
    String sessionId,
    TrainingSessionStatus status,
    Instant createdAt,
    Instant startedAt,
    Instant updatedAt,
    Instant expiresAt,
    String failureReason
) {
}
