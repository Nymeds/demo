package studdy.example.demo.user;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppUserTest {

    @Test
    void acceptsAnyTokenWhileThePasswordWasNeverChanged() {
        AppUser user = new AppUser("Estudante", "token@example.com", "hash");

        assertTrue(user.acceptsTokenIssuedAt(Instant.now().minus(1, ChronoUnit.HOURS)));
    }

    @Test
    void rejectsATokenIssuedBeforeThePasswordChange() {
        AppUser user = new AppUser("Estudante", "token@example.com", "hash");
        user.changePasswordHash("novo-hash");

        assertFalse(user.acceptsTokenIssuedAt(user.getCredentialsUpdatedAt().minusSeconds(5)));
    }

    @Test
    void rejectsATokenIssuedInTheSameSecondAsThePasswordChange() {
        AppUser user = new AppUser("Estudante", "token@example.com", "hash");
        user.changePasswordHash("novo-hash");

        // A troca fica registrada no segundo seguinte ao que ela aconteceu.
        Instant sameSecondAsTheChange = user.getCredentialsUpdatedAt().minusSeconds(1);

        assertFalse(user.acceptsTokenIssuedAt(sameSecondAsTheChange));
    }

    @Test
    void acceptsTheTokenIssuedAtTheRecordedPasswordChange() {
        AppUser user = new AppUser("Estudante", "token@example.com", "hash");
        user.changePasswordHash("novo-hash");

        assertEquals(0, user.getCredentialsUpdatedAt().getNano(), "o JWT só representa segundos inteiros");
        assertTrue(user.acceptsTokenIssuedAt(user.getCredentialsUpdatedAt()));
    }

    @Test
    void rejectsATokenWithoutIssueDateAfterAPasswordChange() {
        AppUser user = new AppUser("Estudante", "token@example.com", "hash");
        user.changePasswordHash("novo-hash");

        assertFalse(user.acceptsTokenIssuedAt(null));
    }
}
