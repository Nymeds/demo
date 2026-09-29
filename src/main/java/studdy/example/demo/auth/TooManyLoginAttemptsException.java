package studdy.example.demo.auth;

import studdy.example.demo.security.TooManyRequestsException;

public class TooManyLoginAttemptsException extends TooManyRequestsException {

    public TooManyLoginAttemptsException(long retryAfterSeconds) {
        super("Muitas tentativas de entrada.", retryAfterSeconds);
    }
}
