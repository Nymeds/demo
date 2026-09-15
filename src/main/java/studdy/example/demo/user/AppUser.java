package studdy.example.demo.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@Entity
@Table(name = "app_users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, name = "password_hash")
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    // Momento da última troca de senha. Tokens emitidos antes dele deixam de valer.
    @Column(name = "credentials_updated_at")
    private Instant credentialsUpdatedAt;

    public AppUser(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public void updateProfile(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // O JWT guarda a emissão em segundos inteiros, então um token emitido no mesmo segundo da troca
    // seria indistinguível do token novo. A troca fica registrada no segundo inteiro seguinte e o
    // token novo é emitido exatamente nesse instante: tudo o que foi emitido antes deixa de valer.
    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.credentialsUpdatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
    }

    public boolean acceptsTokenIssuedAt(Instant issuedAt) {
        if (credentialsUpdatedAt == null) {
            return true;
        }

        return issuedAt != null && !issuedAt.isBefore(credentialsUpdatedAt);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}