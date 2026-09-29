package studdy.example.demo.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptLimiterTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T12:00:00Z"));
    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
        limiter = new LoginAttemptLimiter(clock);
    }

    @Test
    void blocksEmailAndIpPairAfterFiveFailuresAndReportsRetryAfter() {
        fail("a@x.com", "9.9.9.9", 5);

        TooManyLoginAttemptsException error = assertThrows(
                TooManyLoginAttemptsException.class, () -> limiter.acquire("a@x.com", "9.9.9.9"));
        assertEquals(15 * 60, error.getRetryAfterSeconds());
        assertEquals(15, error.getRetryAfterMinutes());
    }

    @Test
    void attackerOnOneIpDoesNotLockTheVictimOutFromAnotherIp() {
        fail("vitima@x.com", "6.6.6.6", 5);

        assertDoesNotThrow(() -> limiter.acquire("vitima@x.com", "1.1.1.1"));
    }

    @Test
    void fourFailuresStillAllowed() {
        fail("a@x.com", "9.9.9.9", 4);
        assertDoesNotThrow(() -> limiter.acquire("a@x.com", "9.9.9.9"));
    }

    @Test
    void successResetsThePairCounter() {
        fail("a@x.com", "9.9.9.9", 4);
        limiter.acquire("a@x.com", "9.9.9.9");
        limiter.recordSuccess("a@x.com", "9.9.9.9");
        fail("a@x.com", "9.9.9.9", 4);
        assertDoesNotThrow(() -> limiter.acquire("a@x.com", "9.9.9.9"));
    }

    @Test
    void blocksIpAfterTwentyFailuresAcrossEmails() {
        for (int i = 0; i < 20; i++) {
            limiter.acquire("user" + i + "@x.com", "7.7.7.7");
        }
        assertThrows(TooManyLoginAttemptsException.class, () -> limiter.acquire("fresh@x.com", "7.7.7.7"));
        assertDoesNotThrow(() -> limiter.acquire("fresh@x.com", "8.8.8.8"));
    }

    @Test
    void distributedAttackHitsThePerEmailBackstop() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL; i++) {
            limiter.acquire("alvo@x.com", "10.0." + (i / 200) + "." + (i % 200));
        }
        assertThrows(TooManyLoginAttemptsException.class, () -> limiter.acquire("alvo@x.com", "11.1.1.1"));
    }

    @Test
    void rejectedAttemptLeavesNoPartialReservation() {
        for (int i = 0; i < 20; i++) {
            limiter.acquire("user" + i + "@x.com", "7.7.7.7");
        }
        // Recusado pelo IP: o par (e-mail + IP) não pode ter ficado contado.
        for (int i = 0; i < 10; i++) {
            assertThrows(TooManyLoginAttemptsException.class, () -> limiter.acquire("b@x.com", "7.7.7.7"));
        }
        fail("b@x.com", "5.5.5.5", 4);
        assertDoesNotThrow(() -> limiter.acquire("b@x.com", "5.5.5.5"));
    }

    @Test
    void windowExpiresWithClock() {
        fail("a@x.com", "9.9.9.9", 5);
        now.set(now.get().plus(Duration.ofMinutes(15)));
        assertDoesNotThrow(() -> limiter.acquire("a@x.com", "9.9.9.9"));
    }

    @Test
    void retryAfterShrinksAsTimePasses() {
        fail("a@x.com", "9.9.9.9", 5);
        now.set(now.get().plus(Duration.ofMinutes(10)));
        TooManyLoginAttemptsException error = assertThrows(
                TooManyLoginAttemptsException.class, () -> limiter.acquire("a@x.com", "9.9.9.9"));
        assertEquals(5, error.getRetryAfterMinutes());
    }

    @Test
    void expiredEntriesAreEvictedByThePeriodicSweep() {
        for (int i = 0; i < 50; i++) {
            limiter.acquire("user" + i + "@x.com", "10.0.0." + i);
        }
        assertEquals(150, limiter.trackedKeys());
        now.set(now.get().plus(Duration.ofMinutes(16)));
        limiter.acquire("other@x.com", "1.2.3.4");
        assertEquals(3, limiter.trackedKeys());
    }

    @Test
    void concurrentAttemptsNeverExceedTheLimit() throws Exception {
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int i = 0; i < threads * 4; i++) {
                Callable<Boolean> attempt = () -> {
                    start.await();
                    try {
                        limiter.acquire("corrida@x.com", "3.3.3.3");
                        return true;
                    } catch (TooManyLoginAttemptsException exception) {
                        return false;
                    }
                };
                results.add(pool.submit(attempt));
            }
            start.countDown();
            int allowed = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    allowed++;
                }
            }
            assertEquals(LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL_AND_IP, allowed);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void memoryStaysBoundedByRejectingNewKeysOverTheCap() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_TRACKED_KEYS; i++) {
            limiter.acquire("user" + i + "@x.com", "ip" + i);
        }
        assertThrows(TooManyLoginAttemptsException.class, () -> limiter.acquire("novo@x.com", "ip-novo"));
        assertTrue(limiter.trackedKeys() <= 3 * LoginAttemptLimiter.MAX_TRACKED_KEYS);
        // Chaves já existentes continuam funcionando normalmente.
        limiter.recordSuccess("user1@x.com", "ip1");
    }

    private void fail(String email, String ip, int times) {
        for (int i = 0; i < times; i++) {
            limiter.acquire(email, ip);
        }
    }
}
