package com.panucci.mlp.services.sessions;

import java.time.Instant;

public record TestSession(
    String testSessionId,
    String trainingSessionId,
    Instant createdAt,
    Instant expiresAt
) {
}