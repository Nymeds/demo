package studdy.example.demo.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contador em memória por chave, em janela fixa iniciada na primeira tentativa.
 *
 * <p>{@link #tryAcquire} reserva uma tentativa de forma atômica (via {@code compute}) antes do
 * trabalho caro, então requisições concorrentes nunca passam do limite. Quem teve sucesso pode
 * devolver a reserva com {@link #release} ou zerar a chave com {@link #reset}.
 *
 * <p>Memória limitada: entradas vencidas são removidas por uma varredura periódica (no máximo uma
 * por minuto, feita por quem chegar primeiro). Nunca há varredura a cada falha. Quando o número de
 * chaves atinge o teto, chaves <em>novas</em> são recusadas (tratadas como bloqueadas) em vez de
 * despejar chaves existentes: despejar permitiria a um atacante apagar o próprio histórico criando
 * chaves descartáveis. O custo é que, sob inundação de chaves, novas chaves ficam bloqueadas até a
 * próxima varredura liberar espaço.
 */
public final class FixedWindowRateLimiter {

    private static final Duration SWEEP_INTERVAL = Duration.ofMinutes(1);

    private record Window(Instant start, int count) {
    }

    private final Clock clock;
    private final int limit;
    private final Duration window;
    private final int maxKeys;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> lastSweep;

    public FixedWindowRateLimiter(Clock clock, int limit, Duration window, int maxKeys) {
        if (limit < 1 || maxKeys < 1 || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("Configuração de limite inválida.");
        }
        this.clock = clock;
        this.limit = limit;
        this.window = window;
        this.maxKeys = maxKeys;
        // Começa na primeira chamada, não na construção: o Clock pode ser um mock ainda sem resposta
        // quando o contexto do Spring sobe (testes de sessão e recuperação controlam o relógio).
        this.lastSweep = new AtomicReference<>();
    }

    /** Reserva uma tentativa. Devolve 0 se reservou; senão, os segundos até a janela vencer. */
    public long tryAcquire(String key) {
        Instant now = clock.instant();
        sweepIfDue(now);
        AtomicBoolean acquired = new AtomicBoolean(false);

        Window result = windows.compute(key, (k, current) -> {
            if (current == null || isExpired(current, now)) {
                if (current == null && windows.size() >= maxKeys) {
                    return null;
                }
                acquired.set(true);
                return new Window(now, 1);
            }
            if (current.count() >= limit) {
                return current;
            }
            acquired.set(true);
            return new Window(current.start(), current.count() + 1);
        });

        if (acquired.get()) {
            return 0;
        }
        return result == null ? window.toSeconds() : retryAfter(result, now);
    }

    /** Segundos até liberar a chave, sem reservar nada (0 = livre). */
    public long retryAfterSeconds(String key) {
        Instant now = clock.instant();
        Window current = windows.get(key);
        if (current == null || isExpired(current, now) || current.count() < limit) {
            return 0;
        }
        return retryAfter(current, now);
    }

    /** Devolve uma reserva feita por {@link #tryAcquire}. */
    public void release(String key) {
        windows.computeIfPresent(key, (k, current) ->
                current.count() <= 1 ? null : new Window(current.start(), current.count() - 1));
    }

    public void reset(String key) {
        windows.remove(key);
    }

    public int size() {
        return windows.size();
    }

    private long retryAfter(Window current, Instant now) {
        return Math.max(1, Duration.between(now, current.start().plus(window)).toSeconds());
    }

    private boolean isExpired(Window current, Instant now) {
        return !now.isBefore(current.start().plus(window));
    }

    private void sweepIfDue(Instant now) {
        Instant previous = lastSweep.get();
        if (previous == null) {
            lastSweep.compareAndSet(null, now);
            return;
        }
        if (Duration.between(previous, now).compareTo(SWEEP_INTERVAL) >= 0
                && lastSweep.compareAndSet(previous, now)) {
            windows.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
        }
    }
}
