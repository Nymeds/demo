package studdy.example.demo.auth;

import org.springframework.stereotype.Component;
import studdy.example.demo.security.FixedWindowRateLimiter;

import java.time.Clock;
import java.time.Duration;

// Limitador em memória contra força bruta no login, em janelas fixas de 15 minutos:
// - por (e-mail + IP): 5 tentativas. É o limite que o estudante sente ao errar a senha.
// - por IP: 20 tentativas, contra um IP testando muitos e-mails.
// - por e-mail (global): 50 tentativas, teto contra ataque distribuído em muitos IPs. Um único IP
//   atacante não consegue bloquear a vítima: ele esbarra antes no limite de 5 (e-mail + IP).
// A tentativa é reservada de forma atômica ANTES do BCrypt (logins concorrentes não passam do
// limite). Em caso de sucesso a reserva é devolvida e o contador (e-mail + IP) é zerado; em caso
// de falha ela fica contada. Memória: ver FixedWindowRateLimiter (varredura periódica; acima do
// teto, chaves novas são recusadas em vez de despejar as existentes).
@Component
public class LoginAttemptLimiter {

    static final int MAX_FAILURES_PER_EMAIL_AND_IP = 5;
    static final int MAX_FAILURES_PER_IP = 20;
    static final int MAX_FAILURES_PER_EMAIL = 50;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final int MAX_TRACKED_KEYS = 10_000;

    private final FixedWindowRateLimiter byEmailAndIp;
    private final FixedWindowRateLimiter byIp;
    private final FixedWindowRateLimiter byEmail;

    public LoginAttemptLimiter(Clock clock) {
        this.byEmailAndIp = new FixedWindowRateLimiter(clock, MAX_FAILURES_PER_EMAIL_AND_IP, WINDOW, MAX_TRACKED_KEYS);
        this.byIp = new FixedWindowRateLimiter(clock, MAX_FAILURES_PER_IP, WINDOW, MAX_TRACKED_KEYS);
        this.byEmail = new FixedWindowRateLimiter(clock, MAX_FAILURES_PER_EMAIL, WINDOW, MAX_TRACKED_KEYS);
    }

    /**
     * Reserva uma tentativa nos três contadores ou lança {@link TooManyLoginAttemptsException}
     * (sem deixar nenhuma reserva parcial para trás).
     */
    public void acquire(String email, String ip) {
        String pair = pairKey(email, ip);

        long retryAfter = byEmailAndIp.tryAcquire(pair);
        if (retryAfter > 0) {
            throw new TooManyLoginAttemptsException(retryAfter);
        }

        retryAfter = byIp.tryAcquire(ip);
        if (retryAfter > 0) {
            byEmailAndIp.release(pair);
            throw new TooManyLoginAttemptsException(retryAfter);
        }

        retryAfter = byEmail.tryAcquire(email);
        if (retryAfter > 0) {
            byEmailAndIp.release(pair);
            byIp.release(ip);
            throw new TooManyLoginAttemptsException(retryAfter);
        }
    }

    /** Login certo: devolve a reserva e zera o contador (e-mail + IP). */
    public void recordSuccess(String email, String ip) {
        byEmailAndIp.reset(pairKey(email, ip));
        byIp.release(ip);
        byEmail.release(email);
    }

    int trackedKeys() {
        return byEmailAndIp.size() + byIp.size() + byEmail.size();
    }

    private static String pairKey(String email, String ip) {
        return email + '|' + ip;
    }
}
