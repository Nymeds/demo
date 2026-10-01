package studdy.example.demo.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import studdy.example.demo.security.FixedWindowRateLimiter;
import studdy.example.demo.security.TooManyRequestsException;

import java.time.Clock;
import java.time.Duration;

// Limita /refresh e /logout (sessão por cookie) por IP: são públicos e consultam o banco a cada
// chamada. Uma aba normal renova a sessão a cada ~15 min, então o limite padrão só barra inundação.
@Component
public class TokenRequestLimiter {

    static final int MAX_TRACKED_KEYS = 10_000;

    private final FixedWindowRateLimiter limiter;

    public TokenRequestLimiter(
            Clock clock,
            @Value("${app.rate-limit.token.max-per-ip:60}") int maxPerIp,
            @Value("${app.rate-limit.token.window:1m}") Duration window
    ) {
        this.limiter = new FixedWindowRateLimiter(clock, maxPerIp, window, MAX_TRACKED_KEYS);
    }

    public void acquire(String clientIp) {
        long retryAfter = limiter.tryAcquire(clientIp);
        if (retryAfter > 0) {
            throw new TooManyRequestsException("Muitas renovações de sessão a partir desta rede.", retryAfter);
        }
    }
}
