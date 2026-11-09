package com.acadezer.api.ratelimit;

import com.acadezer.api.exception.RateLimitExceededException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AiRequestLimiterTests {
    @Test void enforcesGlobalConcurrencyAndDoesNotChargeRejectedUser() {
        var limiter = new AiRequestLimiter(1, Duration.ofHours(1), 1, 1, 100);
        var permit = limiter.acquire("alice");
        assertThatThrownBy(() -> limiter.acquire("bob")).isInstanceOf(RateLimitExceededException.class);
        permit.close();
        try (var other = limiter.acquire("bob")) {
            assertThatThrownBy(() -> limiter.acquire("alice")).isInstanceOf(RateLimitExceededException.class);
        }
    }

    @Test void enforcesPerUserConcurrencyAndPermitClosesOnlyOnce() {
        var limiter = new AiRequestLimiter(2, Duration.ofHours(1), 2, 1, 100);
        var first = limiter.acquire("alice");
        assertThatThrownBy(() -> limiter.acquire("alice")).isInstanceOf(RateLimitExceededException.class);
        first.close(); first.close();
        try (var second = limiter.acquire("alice"); var bob = limiter.acquire("bob")) {
            assertThatThrownBy(() -> limiter.acquire("carol")).isInstanceOf(RateLimitExceededException.class);
        }
        assertThatThrownBy(() -> limiter.acquire("alice")).isInstanceOf(RateLimitExceededException.class);
    }
}
