package studdy.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationInMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms}") long expirationInMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationInMs = expirationInMs;
    }

    public String generateToken(UUID userId) {
        return generateToken(userId, Instant.now());
    }

    public String generateToken(UUID userId, Instant issuedAt) {
        return Jwts.builder()
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

        return new TokenClaims(
                UUID.fromString(claims.getSubject()),
                issuedAt == null ? null : issuedAt.toInstant()
        );
    }

    public record TokenClaims(UUID userId, Instant issuedAt) {
    }

    public long getExpirationInSeconds() {
        return expirationInMs / 1000;
    }
}