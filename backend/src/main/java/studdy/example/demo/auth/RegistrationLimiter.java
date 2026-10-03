package studdy.example.demo.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import studdy.example.demo.security.FixedWindowRateLimiter;
import studdy.example.demo.security.TooManyRequestsException;

import java.time.Clock;
import java.time.Duration;

// Limita cadastros por IP (toda tentativa conta): cada cadastro custa um BCrypt e uma linha no banco.
@Component
public class RegistrationLimiter {

    static final int MAX_TRACKED_KEYS = 10_000;

    private final FixedWindowRateLimiter limiter;

    public RegistrationLimiter(
            Clock clock,
            @Value("${app.rate-limit.register.max-per-ip:10}") int maxPerIp,
            @Value("${app.rate-limit.register.window:1h}") Duration window
    ) {
        this.limiter = new FixedWindowRateLimiter(clock, maxPerIp, window, MAX_TRACKED_KEYS);
    }

    public void acquire(String clientIp) {
        long retryAfter = limiter.tryAcquire(clientIp);
        if (retryAfter > 0) {
            throw new TooManyRequestsException("Muitos cadastros feitos a partir desta rede.", retryAfter);
        }
    }
}
