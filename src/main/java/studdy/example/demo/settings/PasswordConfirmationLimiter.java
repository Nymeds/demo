package studdy.example.demo.settings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import studdy.example.demo.security.FixedWindowRateLimiter;
import studdy.example.demo.security.TooManyRequestsException;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

// Limita a confirmação da senha atual (trocar e-mail, trocar senha, excluir conta) por usuário:
// um access token roubado não vira um oráculo para adivinhar a senha. A tentativa é reservada
// antes do BCrypt e devolvida quando a senha confere; só as falhas ficam contadas.
@Component
public class PasswordConfirmationLimiter {

    static final int MAX_TRACKED_KEYS = 10_000;

    private final FixedWindowRateLimiter limiter;

    public PasswordConfirmationLimiter(
            Clock clock,
            @Value("${app.rate-limit.password-confirmation.max-failures:5}") int maxFailures,
            @Value("${app.rate-limit.password-confirmation.window:15m}") Duration window
    ) {
        this.limiter = new FixedWindowRateLimiter(clock, maxFailures, window, MAX_TRACKED_KEYS);
    }

    public void acquire(UUID userId) {
        long retryAfter = limiter.tryAcquire(userId.toString());
        if (retryAfter > 0) {
            throw new TooManyRequestsException("Muitas tentativas de confirmação de senha.", retryAfter);
        }
    }

    public void recordSuccess(UUID userId) {
        limiter.release(userId.toString());
    }
}
