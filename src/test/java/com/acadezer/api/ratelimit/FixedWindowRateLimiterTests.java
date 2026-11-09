package com.acadezer.api.ratelimit;

import com.acadezer.api.exception.RateLimitExceededException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FixedWindowRateLimiterTests {
    @Test void denialsExpireWithoutExtendingTheWindow() {
        AtomicLong time = new AtomicLong();
        var limiter = new FixedWindowRateLimiter(10, time::get);
        limiter.acquire("account", 2, Duration.ofMinutes(1));
        limiter.acquire("account", 2, Duration.ofMinutes(1));
        assertThatThrownBy(() -> limiter.acquire("account", 2, Duration.ofMinutes(1)))
            .isInstanceOfSatisfying(RateLimitExceededException.class, ex -> assertThat(ex.getRetryAfterSeconds()).isEqualTo(60));
        time.set(Duration.ofSeconds(59).toNanos() + 1);
        assertThatThrownBy(() -> limiter.acquire("account", 2, Duration.ofMinutes(1)))
            .isInstanceOfSatisfying(RateLimitExceededException.class, ex -> assertThat(ex.getRetryAfterSeconds()).isEqualTo(1));
        time.set(Duration.ofMinutes(1).toNanos());
        assertThatCode(() -> limiter.acquire("account", 2, Duration.ofMinutes(1))).doesNotThrowAnyException();
    }

    @Test void boundedStorageDoesNotEvictActiveCountersAndRecoversAfterExpiry() {
        AtomicLong time = new AtomicLong();
        var limiter = new FixedWindowRateLimiter(1, time::get);
        limiter.acquire("first", 1, Duration.ofSeconds(10));
        assertThatThrownBy(() -> limiter.acquire("new", 1, Duration.ofSeconds(10))).isInstanceOf(RateLimitExceededException.class);
        assertThatThrownBy(() -> limiter.acquire("first", 1, Duration.ofSeconds(10))).isInstanceOf(RateLimitExceededException.class);
        time.set(Duration.ofSeconds(10).toNanos());
        assertThatCode(() -> limiter.acquire("new", 1, Duration.ofSeconds(10))).doesNotThrowAnyException();
    }

    @Test void parallelRequestsCannotExceedQuota() throws Exception {
        var limiter = new FixedWindowRateLimiter(10);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(16)) {
            var results = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 32; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    try {
                        limiter.acquire("shared", 5, Duration.ofMinutes(1));
                        return true;
                    } catch (RateLimitExceededException ex) { return false; }
                }));
            }
            start.countDown();
            int accepted = 0;
            for (var result : results) if (result.get(5, TimeUnit.SECONDS)) accepted++;
            assertThat(accepted).isEqualTo(5);
        }
    }

    @Test void countersAreIndependentPerKey() {
        var limiter = new FixedWindowRateLimiter(10);
        limiter.acquire("first", 1, Duration.ofMinutes(1));
        assertThatCode(() -> limiter.acquire("second", 1, Duration.ofMinutes(1))).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.acquire("first", 1, Duration.ofMinutes(1))).isInstanceOf(RateLimitExceededException.class);
    }
}
