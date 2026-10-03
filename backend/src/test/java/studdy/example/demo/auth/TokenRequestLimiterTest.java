package studdy.example.demo.auth;

import org.junit.jupiter.api.Test;
import studdy.example.demo.security.TooManyRequestsException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenRequestLimiterTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void blocksAnIpAfterTheLimitWithoutAffectingOtherIps() {
        TokenRequestLimiter limiter = new TokenRequestLimiter(clock, 3, Duration.ofMinutes(1));

        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> limiter.acquire("10.0.0.1"));
        }

        assertThrows(TooManyRequestsException.class, () -> limiter.acquire("10.0.0.1"));
        assertDoesNotThrow(() -> limiter.acquire("10.0.0.2"));
    }
}
