package com.acadezer.api.ratelimit;

import com.acadezer.api.exception.RateLimitExceededException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Bounded, process-local counters. Denials never extend the window. */
public final class FixedWindowRateLimiter {
    private final Map<String, Window> windows = new HashMap<>();
    private final int maxKeys;
    private final LongSupplier ticker;

    public FixedWindowRateLimiter(int maxKeys) {
        this(maxKeys, System::nanoTime);
    }

    FixedWindowRateLimiter(int maxKeys, LongSupplier ticker) {
        if (maxKeys < 1) throw new IllegalArgumentException("maxKeys debe ser positivo");
        this.maxKeys = maxKeys;
        this.ticker = ticker;
    }

    public synchronized void acquire(String key, int limit, Duration duration) {
        if (limit < 1 || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Los límites y sus ventanas deben ser positivos");
        }
        long now = ticker.getAsLong();
        Window window = windows.get(key);
        if (window != null && now - window.started >= window.durationNanos) {
            windows.remove(key);
            window = null;
        }
        if (window == null) {
            if (windows.size() >= maxKeys) {
                windows.values().removeIf(w -> now - w.started >= w.durationNanos);
                if (windows.size() >= maxKeys) {
                    long remaining = windows.values().stream()
                        .mapToLong(w -> w.durationNanos - (now - w.started)).min().orElseThrow();
                    throw new RateLimitExceededException(seconds(remaining));
                }
            }
            window = new Window(now, duration.toNanos());
            windows.put(key, window);
        }
        if (window.used >= limit) {
            throw new RateLimitExceededException(seconds(window.durationNanos - (now - window.started)));
        }
        window.used++;
    }

    private static long seconds(long nanos) {
        return Math.max(1, (nanos - 1) / 1_000_000_000L + 1);
    }

    private static final class Window {
        final long started;
        final long durationNanos;
        int used;

        Window(long started, long durationNanos) {
            this.started = started;
            this.durationNanos = durationNanos;
        }
    }
}
