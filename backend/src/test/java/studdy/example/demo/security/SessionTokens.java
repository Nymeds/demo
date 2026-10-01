package studdy.example.demo.security;

import java.util.UUID;
import studdy.example.demo.auth.session.BrowserSessionService;

/** Helper de testes: access token vinculado a uma sessão de navegador real (claim "sid"). */
public final class SessionTokens {
    private SessionTokens() {}

    public static String of(JwtService jwt, BrowserSessionService sessions, UUID userId) {
        var session = sessions.create(userId, false, null);
        return jwt.generateToken(userId, java.time.Instant.now(), session.sessionId());
    }
}
