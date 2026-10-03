package studdy.example.demo.auth.recovery;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class RecoveryRateLimiter {
    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<>();

    public RecoveryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized void check(String address) {
        Instant now = clock.instant();
        windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt));
        Window window = windows.get(address);
        if (window == null) {
            if (windows.size() >= 10_000) throw limited();
            window = new Window(now.plusSeconds(3600));
            windows.put(address, window);
        }
        if (window.count >= 30) throw limited();
        window.count++;
    }

    private ResponseStatusException limited() {
        return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Muitas solicitações de recuperação. Aguarde até uma hora e tente novamente.");
    }

    private static class Window {
        private final Instant expiresAt;
        private int count;
        private Window(Instant expiresAt) { this.expiresAt = expiresAt; }
    }
}
