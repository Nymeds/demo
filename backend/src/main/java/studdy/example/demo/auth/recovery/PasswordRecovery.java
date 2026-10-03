package studdy.example.demo.auth.recovery;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import studdy.example.demo.user.AppUser;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "password_recoveries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordRecovery {
    @Id
    private UUID id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AppUser user;

    private String email;
    private Instant credentialsUpdatedAt;
    private UUID nonce;
    private String codeHash;
    private Instant codeExpiresAt;
    private int failedAttempts;
    private String tokenHash;
    private Instant tokenExpiresAt;
    private Instant lastSentAt;
    private Instant sendWindowStartedAt;
    private int sendsInWindow;

    public PasswordRecovery(AppUser user) {
        this.user = user;
    }

    public boolean canSend(Instant now) {
        return (lastSentAt == null || !now.isBefore(lastSentAt.plusSeconds(60)))
                && (sendWindowStartedAt == null || !now.isBefore(sendWindowStartedAt.plusSeconds(3600))
                || sendsInWindow < 5);
    }

    public void issue(UUID nonce, String codeHash, Instant now) {
        if (sendWindowStartedAt == null || !now.isBefore(sendWindowStartedAt.plusSeconds(3600))) {
            sendWindowStartedAt = now;
            sendsInWindow = 0;
        }
        sendsInWindow++;
        lastSentAt = now;
        this.email = user.getEmail();
        this.credentialsUpdatedAt = user.getCredentialsUpdatedAt();
        this.nonce = nonce;
        this.codeHash = codeHash;
        codeExpiresAt = now.plusSeconds(600);
        failedAttempts = 0;
        tokenHash = null;
        tokenExpiresAt = null;
    }

    public boolean matchesAccount() {
        return Objects.equals(email, user.getEmail())
                && Objects.equals(credentialsUpdatedAt, user.getCredentialsUpdatedAt());
    }

    public boolean canVerify(Instant now) {
        return codeHash != null && failedAttempts < 5 && now.isBefore(codeExpiresAt) && matchesAccount();
    }

    public void rejectCode() {
        failedAttempts++;
        if (failedAttempts >= 5) codeHash = null;
    }

    public void authorizeReset(String hash, Instant now) {
        codeHash = null;
        tokenHash = hash;
        tokenExpiresAt = now.plusSeconds(300);
    }

    public boolean canReset(Instant now) {
        return tokenHash != null && now.isBefore(tokenExpiresAt) && matchesAccount();
    }

    public void consume() {
        codeHash = null;
        tokenHash = null;
        tokenExpiresAt = null;
    }
}
