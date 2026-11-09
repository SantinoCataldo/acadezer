package com.acadezer.api.ratelimit;

import com.acadezer.api.exception.RateLimitExceededException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiRequestLimiter {
    private final FixedWindowRateLimiter quota;
    private final int requests;
    private final Duration window;
    private final int maxConcurrent;
    private final int maxConcurrentPerUser;
    private final Map<String, Integer> activeByUser = new HashMap<>();
    private int active;

    public AiRequestLimiter(
            @Value("${app.limits.ai.requests}") int requests,
            @Value("${app.limits.ai.window}") Duration window,
            @Value("${app.limits.ai.max-concurrent}") int maxConcurrent,
            @Value("${app.limits.ai.max-concurrent-per-user}") int maxConcurrentPerUser,
            @Value("${app.limits.max-keys}") int maxKeys) {
        if (requests < 1 || maxConcurrent < 1 || maxConcurrentPerUser < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("La configuración de límites de IA debe ser positiva");
        }
        this.quota = new FixedWindowRateLimiter(maxKeys);
        this.requests = requests;
        this.window = window;
        this.maxConcurrent = maxConcurrent;
        this.maxConcurrentPerUser = maxConcurrentPerUser;
    }

    public synchronized Permit acquire(String user) {
        if (active >= maxConcurrent || activeByUser.getOrDefault(user, 0) >= maxConcurrentPerUser) {
            throw new RateLimitExceededException(1);
        }
        // Check quota before reserving a slot; concurrency denials consume no quota.
        quota.acquire(user, requests, window);
        active++;
        activeByUser.merge(user, 1, Integer::sum);
        return new Permit(user);
    }

    public final class Permit implements AutoCloseable {
        private final String user;
        private boolean closed;

        private Permit(String user) {
            this.user = user;
        }

        @Override
        public void close() {
            synchronized (AiRequestLimiter.this) {
                if (closed) return;
                closed = true;
                active--;
                int remaining = activeByUser.get(user) - 1;
                if (remaining == 0) activeByUser.remove(user);
                else activeByUser.put(user, remaining);
            }
        }
    }
}
