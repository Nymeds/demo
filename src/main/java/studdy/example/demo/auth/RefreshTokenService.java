package studdy.example.demo.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 32;
    static final Duration PURGE_RETENTION = Duration.ofDays(1);

    public record IssuedToken(String token, Instant expiresAt) {
    }

    public record RotatedToken(UUID userId, Instant credentialsUpdatedAt, String token, Instant expiresAt) {
    }

    private record Stored(RefreshToken entity, IssuedToken issued) {
    }

    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final Duration rememberMeTtl;
    private final Duration sessionTtl;
    private final Duration reuseGrace;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository repository,
            UserRepository userRepository,
            Clock clock,
            @Value("${app.refresh-token.remember-me-ttl:30d}") Duration rememberMeTtl,
            @Value("${app.refresh-token.session-ttl:12h}") Duration sessionTtl,
            @Value("${app.refresh-token.reuse-grace:10s}") Duration reuseGrace
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.rememberMeTtl = rememberMeTtl;
        this.sessionTtl = sessionTtl;
        this.reuseGrace = reuseGrace;
    }

    /** Abre uma nova família de tokens (um login). */
    @Transactional
    public IssuedToken issue(AppUser user, boolean rememberMe) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(rememberMe ? rememberMeTtl : sessionTtl);
        return store(user, UUID.randomUUID(), expiresAt, now).issued();
    }

    /**
     * Troca o token por um novo da mesma família, com o mesmo vencimento absoluto.
     *
     * <p>Concorrência: trava o usuário e depois a linha do token (mesma ordem da troca de senha),
     * então duas rotações simultâneas do mesmo token são serializadas e só uma vence.
     *
     * <p>Reapresentar um token já rotacionado há menos de {@code reuseGrace} é o caso legítimo de
     * duas abas renovando juntas: 401 sem derrubar a família. Depois da janela (ou um token revogado
     * por logout/troca de senha) indica roubo ou cópia: a família inteira é revogada. A revogação
     * precisa ser gravada mesmo com a exceção, por isso o noRollbackFor.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotatedToken rotate(String rawToken) {
        String tokenHash = hash(rawToken);
        UUID userId = repository.findUserIdByTokenHash(tokenHash).orElseThrow(InvalidRefreshTokenException::new);
        AppUser user = userRepository.findByIdForUpdate(userId).orElseThrow(InvalidRefreshTokenException::new);
        RefreshToken current = repository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = clock.instant();

        if (current.isRevoked()) {
            if (!current.wasRotatedWithin(now, reuseGrace)) {
                revokeFamily(current.getFamilyId(), now);
            }
            throw new InvalidRefreshTokenException();
        }

        if (current.isExpiredAt(now)) {
            throw new InvalidRefreshTokenException();
        }

        Stored next = store(user, current.getFamilyId(), current.getExpiresAt(), now);
        current.revoke(now, next.entity().getId());

        return new RotatedToken(user.getId(), user.getCredentialsUpdatedAt(), next.issued().token(),
                next.issued().expiresAt());
    }

    /** Logout: revoga a família inteira do token. Idempotente: token desconhecido não é erro. */
    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> {
                    Instant now = clock.instant();
                    token.revoke(now, null);
                    revokeFamily(token.getFamilyId(), now);
                });
    }

    /**
     * Revoga todas as sessões do usuário. Trava o usuário antes (mesma ordem de {@link #rotate}),
     * então uma rotação em andamento termina antes e o token que ela criou também é revogado.
     */
    @Transactional
    public void revokeAllForUser(UUID userId) {
        userRepository.findByIdForUpdate(userId);
        Instant now = clock.instant();
        repository.findActiveByUserIdForUpdate(userId).forEach(token -> token.revoke(now, null));
    }

    @Transactional
    public void deleteAllForUser(UUID userId) {
        repository.deleteByUser_Id(userId);
    }

    /** Limpeza diária: tokens vencidos ou revogados há mais de um dia não servem mais para nada. */
    @Scheduled(cron = "${app.refresh-token.purge-cron:0 30 3 * * *}")
    @Transactional
    public int purgeStale() {
        int removed = repository.deleteExpiredOrRevokedBefore(clock.instant().minus(PURGE_RETENTION));
        if (removed > 0) {
            log.info("Limpeza de refresh tokens: {} removido(s).", removed);
        }
        return removed;
    }

    private void revokeFamily(UUID familyId, Instant now) {
        repository.findAllByFamilyIdAndRevokedAtIsNull(familyId).forEach(token -> token.revoke(now, null));
    }

    private Stored store(AppUser user, UUID familyId, Instant expiresAt, Instant now) {
        String raw = generateRawToken();
        RefreshToken saved = repository.save(new RefreshToken(user, hash(raw), familyId, expiresAt, now));
        return new Stored(saved, new IssuedToken(raw, expiresAt));
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }
}
