package com.panucci.mlp.services.sessions;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TestSessionService {

    private final ConcurrentHashMap<String, TestSession> sessions = new ConcurrentHashMap<>();
    private final SessionIdGenerator sessionIdGenerator;
    private final Duration sessionTtl;

    public TestSessionService(
        SessionIdGenerator sessionIdGenerator,
        @Value("${mlp.testing.session-ttl-ms:600000}") long sessionTtlMillis
    ) {
        this.sessionIdGenerator = sessionIdGenerator;
        this.sessionTtl = Duration.ofMillis(sessionTtlMillis);
    }

    public TestSession createSession(String trainingSessionId) {
        while (true) {
            Instant now = Instant.now();
            String testSessionId = this.sessionIdGenerator.generate();
            TestSession session = new TestSession(
                testSessionId,
                trainingSessionId,
                now,
                now.plus(this.sessionTtl)
            );

            if (this.sessions.putIfAbsent(testSessionId, session) == null) {
                return session;
            }
        }
    }

    public Optional<TestSession> findById(String testSessionId) {
        return Optional.ofNullable(this.sessions.get(testSessionId));
    }

    @Scheduled(fixedDelayString = "${mlp.testing.session-cleanup-interval-ms:30000}")
    public void cleanupSessions() {
        Instant now = Instant.now();
        this.sessions.values().removeIf(session -> session.expiresAt().isBefore(now));
    }
}