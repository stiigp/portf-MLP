package com.panucci.mlp.services.sessions;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.panucci.mlp.dto.TrainingSessionStatusEvent;
import com.panucci.mlp.services.publishing.TrainingEventPublisher;

@Service
public class TrainingSessionService {

    private static final String SESSION_STATUS_EVENT_TYPE = "SESSION_STATUS";

    private final ConcurrentHashMap<String, TrainingSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Future<?>> tasks = new ConcurrentHashMap<>();
    private final SessionIdGenerator sessionIdGenerator;
    private final TrainingEventPublisher eventPublisher;
    private final Duration createdSessionTtl;
    private final Duration maxRunningDuration;
    private final Duration terminalSessionTtl;
    private final int maxSessions;

    public TrainingSessionService(
        SessionIdGenerator sessionIdGenerator,
        TrainingEventPublisher eventPublisher,
        @Value("${mlp.training.created-session-ttl-ms:120000}") long createdSessionTtlMillis,
        @Value("${mlp.training.max-running-duration-ms:600000}") long maxRunningDurationMillis,
        @Value("${mlp.training.terminal-session-ttl-ms:600000}") long terminalSessionTtlMillis,
        @Value("${mlp.training.max-sessions:50}") int maxSessions
    ) {
        this.sessionIdGenerator = sessionIdGenerator;
        this.eventPublisher = eventPublisher;
        this.createdSessionTtl = Duration.ofMillis(createdSessionTtlMillis);
        this.maxRunningDuration = Duration.ofMillis(maxRunningDurationMillis);
        this.terminalSessionTtl = Duration.ofMillis(terminalSessionTtlMillis);
        this.maxSessions = maxSessions;
    }

    public TrainingSession createSession() {
        if (this.sessions.size() >= this.maxSessions) {
            throw new IllegalStateException("Training session capacity is full");
        }

        while (true) {
            Instant now = Instant.now();
            String sessionId = this.sessionIdGenerator.generate();
            TrainingSession session = new TrainingSession(
                sessionId,
                TrainingSessionStatus.CREATED,
                now,
                null,
                now,
                now.plus(this.createdSessionTtl),
                null
            );

            if (this.sessions.putIfAbsent(sessionId, session) == null) {
                this.publishStatus(session);
                return session;
            }
        }
    }

    public Optional<TrainingSession> findById(String sessionId) {
        return Optional.ofNullable(this.sessions.get(sessionId));
    }

    public void attachTask(String sessionId, Future<?> task) {
        TrainingSession session = this.sessions.get(sessionId);
        if (session != null && !this.isTerminal(session) && !task.isDone()) {
            this.tasks.put(sessionId, task);
        }
    }

    public TrainingSession markQueued(String sessionId) {
        Instant now = Instant.now();
        TrainingSession updated = this.sessions.compute(sessionId, (id, session) -> {
            if (session == null) {
                throw new NoSuchElementException("Training session does not exist");
            }

            if (session.status() != TrainingSessionStatus.CREATED) {
                throw new IllegalStateException("Training session is not ready to start");
            }

            return new TrainingSession(
                session.sessionId(),
                TrainingSessionStatus.QUEUED,
                session.createdAt(),
                null,
                now,
                null,
                null
            );
        });

        this.publishStatus(updated);
        return updated;
    }

    public void markRunning(String sessionId) {
        Instant now = Instant.now();
        TrainingSession updated = this.sessions.computeIfPresent(sessionId, (id, session) ->
            new TrainingSession(
                session.sessionId(),
                TrainingSessionStatus.RUNNING,
                session.createdAt(),
                now,
                now,
                null,
                null
            )
        );
        this.publishStatus(updated);
    }

    public void markFinished(String sessionId) {
        Instant now = Instant.now();
        this.tasks.remove(sessionId);
        TrainingSession updated = this.sessions.computeIfPresent(sessionId, (id, session) ->
            new TrainingSession(
                session.sessionId(),
                TrainingSessionStatus.FINISHED,
                session.createdAt(),
                session.startedAt(),
                now,
                now.plus(this.terminalSessionTtl),
                null
            )
        );
        this.publishStatus(updated);
    }

    public void markFailed(String sessionId, String failureReason) {
        Instant now = Instant.now();
        this.tasks.remove(sessionId);
        TrainingSession updated = this.sessions.computeIfPresent(sessionId, (id, session) ->
            new TrainingSession(
                session.sessionId(),
                TrainingSessionStatus.FAILED,
                session.createdAt(),
                session.startedAt(),
                now,
                now.plus(this.terminalSessionTtl),
                failureReason
            )
        );
        this.publishStatus(updated);
    }

    public void markRejected(String sessionId, String failureReason) {
        Instant now = Instant.now();
        this.tasks.remove(sessionId);
        TrainingSession updated = this.sessions.computeIfPresent(sessionId, (id, session) ->
            new TrainingSession(
                session.sessionId(),
                TrainingSessionStatus.REJECTED,
                session.createdAt(),
                session.startedAt(),
                now,
                now.plus(this.terminalSessionTtl),
                failureReason
            )
        );
        this.publishStatus(updated);
    }

    @Scheduled(fixedDelayString = "${mlp.training.session-cleanup-interval-ms:30000}")
    public void cleanupSessions() {
        Instant now = Instant.now();

        for (TrainingSession session : this.sessions.values()) {
            if (this.isRunningTooLong(session, now)) {
                this.cancelAndRemove(session.sessionId());
            } else if (this.isExpired(session, now)) {
                this.sessions.remove(session.sessionId(), session);
            }
        }
    }

    private boolean isRunningTooLong(TrainingSession session, Instant now) {
        return session.status() == TrainingSessionStatus.RUNNING
            && session.startedAt() != null
            && session.startedAt().plus(this.maxRunningDuration).isBefore(now);
    }

    private boolean isExpired(TrainingSession session, Instant now) {
        return session.expiresAt() != null && session.expiresAt().isBefore(now);
    }

    private boolean isTerminal(TrainingSession session) {
        return session.status() == TrainingSessionStatus.CREATED
            || session.status() == TrainingSessionStatus.FINISHED
            || session.status() == TrainingSessionStatus.FAILED
            || session.status() == TrainingSessionStatus.REJECTED;
    }

    private void cancelAndRemove(String sessionId) {
        Future<?> task = this.tasks.remove(sessionId);
        if (task != null) {
            task.cancel(true);
        }

        TrainingSession removed = this.sessions.remove(sessionId);
        if (removed != null) {
            this.publishStatus(
                new TrainingSession(
                    removed.sessionId(),
                    TrainingSessionStatus.FAILED,
                    removed.createdAt(),
                    removed.startedAt(),
                    Instant.now(),
                    null,
                    "Training exceeded max running duration"
                )
            );
        }
    }

    private void publishStatus(TrainingSession session) {
        if (session == null) {
            return;
        }

        this.eventPublisher.publish(
            new TrainingSessionStatusEvent(
                SESSION_STATUS_EVENT_TYPE,
                session.sessionId(),
                session.status(),
                session.failureReason()
            )
        );
    }
}
