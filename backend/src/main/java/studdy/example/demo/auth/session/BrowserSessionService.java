package studdy.example.demo.auth.session;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.auth.dto.AuthResponse;
import studdy.example.demo.security.JwtService;
import studdy.example.demo.user.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class BrowserSessionService {
    private static final Duration REMEMBERED_LIFETIME = Duration.ofDays(30);
    private static final Duration BROWSER_LIFETIME = Duration.ofHours(12);
    private final BrowserSessionRepository sessions;
    private final UserRepository users;
    private final JwtService jwt;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public BrowserSessionService(BrowserSessionRepository sessions, UserRepository users, JwtService jwt, Clock clock) {
        this.sessions = sessions;
        this.users = users;
        this.jwt = jwt;
        this.clock = clock;
    }

    @Transactional
    public IssuedSession createAuthenticated(String accessToken, boolean rememberMe, String previousToken) {
        var claims = jwt.parse(accessToken);
        var user = users.findByIdForUpdate(claims.userId()).orElseThrow(this::expired);
        if (!user.acceptsTokenIssuedAt(claims.issuedAt())) throw expired();
        return create(user.getId(), rememberMe, previousToken);
    }

    @Transactional
    public IssuedSession create(UUID userId, boolean rememberMe, String previousToken) {
        var user = users.findByIdForUpdate(userId).orElseThrow(this::expired);
        revoke(previousToken);
        Instant now = now();
        var existing = sessions.findByUserIdOrderByCreatedAtDesc(userId);
        // Limita armazenamento a dez sessões por conta e descarta as expiradas/revogadas.
        int retained = 0;
        for (var session : existing) {
            if (!session.isValid(now) || retained >= 9) sessions.delete(session);
            else retained++;
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = now.plus(rememberMe ? REMEMBERED_LIFETIME : BROWSER_LIFETIME);
        var saved = sessions.save(new BrowserSession(user, hash(token), rememberMe, now, expiresAt));
        return new IssuedSession(token, rememberMe, expiresAt, saved.getId());
    }

    @Transactional
    public SessionResponse refresh(String token) {
        var session = find(token);
        if (session == null) throw expired();
        users.findByIdForUpdate(session.getUser().getId()).orElseThrow(this::expired);
        session = find(token);
        if (session == null || !session.isValid(now())) throw expired();
        var user = session.getUser();
        Instant issuedAt = now();
        if (user.getCredentialsUpdatedAt() != null && user.getCredentialsUpdatedAt().isAfter(issuedAt)) {
            issuedAt = user.getCredentialsUpdatedAt();
        }
        return new SessionResponse(jwt.generateToken(user.getId(), issuedAt, session.getId()), "Bearer", jwt.getExpirationInSeconds(), session.isRememberMe());
    }

    @Transactional
    public void revoke(String token) {
        var session = find(token);
        if (session != null) sessions.delete(session);
    }

    /**
     * Mantém somente o navegador atual conectado após troca de senha autenticada. Não usa
     * isValid(): quando este método roda, credentialsUpdatedAt já mudou e a sessão atual
     * seria sempre recusada; por isso só confere dono e prazo.
     */
    @Transactional
    public IssuedSession replaceAfterPasswordChange(UUID userId, String token) {
        var previous = find(token);
        if (previous == null || !previous.getUser().getId().equals(userId) || !now().isBefore(previous.getExpiresAt())) {
            // Sem cookie válido deste navegador: abre uma sessão nova para que o token emitido tenha "sid".
            return create(userId, false, null);
        }
        return create(userId, previous.isRememberMe(), token);
    }

    /** Consulta única por chave primária usada pelo filtro JWT para tokens com claim "sid". */
    @Transactional(readOnly = true)
    public boolean isActive(UUID sessionId, UUID userId) {
        return sessions.existsByIdAndUser_IdAndExpiresAtAfter(sessionId, userId, now());
    }

    /** Remove sessões expiradas (rodado diariamente por SessionPurgeScheduler). */
    @Transactional
    public int purgeExpired() {
        return sessions.deleteExpired(now());
    }

    private BrowserSession find(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return null;
        return sessions.findByTokenHash(hash(token)).orElse(null);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MILLIS); }
    private ResponseStatusException expired() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sua sessão expirou. Entre novamente.");
    }
    public record IssuedSession(String token, boolean rememberMe, Instant expiresAt, UUID sessionId) {}

    /** Reemite o access token (mesmo usuário e iat) vinculado à sessão criada na mesma requisição (claim "sid"). */
    public AuthResponse bindAccessToken(AuthResponse auth, IssuedSession session) {
        var claims = jwt.parse(auth.accessToken());
        return new AuthResponse(jwt.generateToken(claims.userId(), claims.issuedAt(), session.sessionId()),
                auth.tokenType(), auth.expiresIn());
    }
    public record SessionResponse(String accessToken, String tokenType, long expiresIn, boolean rememberMe) {}
}
