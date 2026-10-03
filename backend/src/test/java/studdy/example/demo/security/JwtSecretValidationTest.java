package studdy.example.demo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class JwtSecretValidationTest {

    @Test
    void rejectsMissingSecret() {
        assertThatThrownBy(() -> new JwtService("", 1000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> new JwtService(null, 1000L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService("curto", 1000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void refusesTheDevSecretOutsideH2() {
        assertThatThrownBy(() -> new JwtService(JwtService.DEV_FALLBACK_SECRET, 1000L,
                "jdbc:postgresql://localhost:5432/academic_organizer"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> new JwtService(JwtService.DEV_FALLBACK_SECRET, 1000L, ""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesSecretsThatWerePublishedInTheRepository() {
        // O texto antigo de exemplo do application.properties; o outro segredo público só existe como hash.
        assertThatThrownBy(() -> new JwtService("troque-esta-chave-local-por-uma-chave-com-mais-de-32-bytes", 1000L,
                "jdbc:h2:mem:academic-organizer"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("publicado");
    }

    @Test
    void acceptsTheDevSecretWithH2AndAnyOtherSecretWithPostgres() {
        new JwtService(JwtService.DEV_FALLBACK_SECRET, 1000L, "jdbc:h2:mem:academic-organizer");
        new JwtService("x".repeat(32), 1000L, "jdbc:postgresql://localhost:5432/academic_organizer");
    }

    @Test
    void issuedAtNeverPrecedesTheCredentialChange() {
        JwtService service = new JwtService("x".repeat(32), 60_000L);
        java.time.Instant future = java.time.Instant.now().plusSeconds(1).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        assertThat(service.parse(service.generateTokenFor(java.util.UUID.randomUUID(), future)).issuedAt())
                .isEqualTo(future);
        java.time.Instant past = java.time.Instant.now().minusSeconds(60);
        assertThat(service.parse(service.generateTokenFor(java.util.UUID.randomUUID(), past)).issuedAt())
                .isAfter(past);
    }

    @Test
    void contextFailsWithoutSecret() {
        new ApplicationContextRunner()
                .withUserConfiguration(JwtService.class)
                .withPropertyValues("app.jwt.secret=", "app.jwt.access-token-expiration-ms=1000")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void contextStartsWithValidSecret() {
        new ApplicationContextRunner()
                .withUserConfiguration(JwtService.class)
                .withPropertyValues("app.jwt.secret=" + "x".repeat(32),
                        "app.jwt.access-token-expiration-ms=1000")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
