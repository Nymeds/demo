package studdy.example.demo.auth.session;

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
@Table(name = "browser_sessions", indexes = @Index(name = "idx_browser_session_user", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrowserSession {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AppUser user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(nullable = false)
    private boolean rememberMe;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant credentialsUpdatedAt;

    public BrowserSession(AppUser user, String tokenHash, boolean rememberMe, Instant now, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.rememberMe = rememberMe;
        this.createdAt = now;
        this.expiresAt = expiresAt;
        this.credentialsUpdatedAt = user.getCredentialsUpdatedAt();
    }

    public boolean isValid(Instant now) {
        return now.isBefore(expiresAt) && Objects.equals(credentialsUpdatedAt, user.getCredentialsUpdatedAt());
    }
}
