package studdy.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    static final int MIN_SECRET_BYTES = 32;
    // Segredo público do perfil dev (application-dev.properties). Só pode rodar com H2.
    static final String DEV_FALLBACK_SECRET = "dev-only-insecure-jwt-secret-do-not-use-in-production";
    // SHA-256 de segredos que já ficaram públicos no histórico do git (o JWT_SECRET do .env.example
    // de ee4d439 e o antigo "troque-esta-chave-local-..."). Guardamos só o hash para não publicá-los de novo.
    static final Set<String> PUBLIC_SECRET_HASHES = Set.of(
            "2a2865d4d32d7ed54924cfe6e09245a9c354ece762601626d0922d338145fd3e",
            "c6213fa2fde7811434b24994a9651ad94a3b7d49bf70f75666064756d427aa3a"
    );

    static final String SESSION_CLAIM = "sid";

    private final SecretKey signingKey;
    private final long expirationInMs;

    public JwtService(String secret, long expirationInMs) {
        this(secret, expirationInMs, "");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms}") long expirationInMs,
            @Value("${spring.datasource.url:}") String datasourceUrl
    ) {
        requireNotDevSecretOutsideH2(secret, datasourceUrl);
        requireNotPublicSecret(secret);
        this.signingKey = Keys.hmacShaKeyFor(requireValidSecret(secret));
        this.expirationInMs = expirationInMs;
    }

    static byte[] requireValidSecret(String secret) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET (app.jwt.secret) ausente ou com menos de " + MIN_SECRET_BYTES
                            + " bytes. Defina a variavel de ambiente JWT_SECRET com um segredo forte "
                            + "(o perfil 'dev' serve apenas para desenvolvimento local).");
        }
        return bytes;
    }

    static void requireNotPublicSecret(String secret) {
        if (secret == null) return;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            if (PUBLIC_SECRET_HASHES.contains(HexFormat.of().formatHex(digest))) {
                throw new IllegalStateException(
                        "Este JWT_SECRET já foi publicado no repositório e não protege nada. "
                                + "Gere um novo segredo (o npm run init gera um sozinho) e defina JWT_SECRET.");
            }
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível na JVM.", exception);
        }
    }

    static void requireNotDevSecretOutsideH2(String secret, String datasourceUrl) {
        boolean h2 = datasourceUrl != null && datasourceUrl.startsWith("jdbc:h2:");
        if (DEV_FALLBACK_SECRET.equals(secret) && !h2) {
            throw new IllegalStateException(
                    "O segredo JWT de desenvolvimento só pode ser usado com o banco H2 do perfil 'dev'. "
                            + "Defina a variavel de ambiente JWT_SECRET com um segredo forte.");
        }
    }

    /**
     * Emite o token com iat = max(agora, credentialsUpdatedAt). A troca de senha/e-mail fica
     * registrada no segundo inteiro seguinte; sem o max, um login feito no mesmo segundo teria iat
     * (truncado em segundos) anterior à troca e seria recusado.
     */
    public String generateTokenFor(UUID userId, Instant credentialsUpdatedAt) {
        Instant now = Instant.now();
        Instant issuedAt = credentialsUpdatedAt != null && credentialsUpdatedAt.isAfter(now) ? credentialsUpdatedAt : now;
        return generateToken(userId, issuedAt);
    }

    public String generateToken(UUID userId) {
        return generateToken(userId, Instant.now());
    }

    public String generateToken(UUID userId, Instant issuedAt) {
        return generateToken(userId, issuedAt, null);
    }

    /** Token vinculado a uma sessão de navegador (claim "sid"): cai junto com a sessão. */
    public String generateToken(UUID userId, Instant issuedAt, UUID sessionId) {
        var builder = Jwts.builder();
        if (sessionId != null) builder.claim(SESSION_CLAIM, sessionId.toString());
        return builder
                .subject(userId.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(expirationInMs)))
                .signWith(signingKey)
                .compact();
    }

    public UUID getUserId(String token) {
        return parse(token).userId();
    }

    public TokenClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Date issuedAt = claims.getIssuedAt();
        String sessionId = claims.get(SESSION_CLAIM, String.class);

        return new TokenClaims(
                UUID.fromString(claims.getSubject()),
                issuedAt == null ? null : issuedAt.toInstant(),
                sessionId == null ? null : UUID.fromString(sessionId)
        );
    }

    /** sessionId é nulo para tokens sem vínculo de sessão (resposta do login e da troca de credenciais). */
    public record TokenClaims(UUID userId, Instant issuedAt, UUID sessionId) {
    }

    public long getExpirationInSeconds() {
        return expirationInMs / 1000;
    }
}