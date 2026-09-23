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
    MLP trainedModel,
    TestDataset testDataset
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
        this(
            sessionId,
            status,
            createdAt,
            startedAt,
            updatedAt,
            expiresAt,
            failureReason,
            queuePosition,
            null,
            null
        );
    }

    public TrainingSession(
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
        this(
            sessionId,
            status,
            createdAt,
            startedAt,
            updatedAt,
            expiresAt,
            failureReason,
            queuePosition,
            trainedModel,
            null
        );
    }
}