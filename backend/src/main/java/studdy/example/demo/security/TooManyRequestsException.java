package studdy.example.demo.security;

// Tentativas demais: vira 429 com Retry-After e ProblemDetail em pt-BR (ValidationExceptionHandler).
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;
    private final String detailPrefix;

    /** @param detailPrefix início da mensagem; o handler completa com "Tente novamente em N minutos." */
    public TooManyRequestsException(String detailPrefix, long retryAfterSeconds) {
        super(detailPrefix);
        this.detailPrefix = detailPrefix;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public long getRetryAfterMinutes() {
        return Math.max(1, (retryAfterSeconds + 59) / 60);
    }

    public String getDetail() {
        long minutes = getRetryAfterMinutes();
        return detailPrefix + " Tente novamente em " + minutes + " " + (minutes == 1 ? "minuto" : "minutos") + ".";
    }
}
