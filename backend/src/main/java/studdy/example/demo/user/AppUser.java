package studdy.example.demo.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
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

    @Column(unique = true, length = 30)
    private String username;

    @Column(length = 20)
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private Gender gender;

    @Column(length = 120)
    private String location;

    @Column(nullable = false, name = "password_hash")
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    // Momento da última troca de senha ou de e-mail. Tokens emitidos antes dele deixam de valer.
    @Column(name = "credentials_updated_at")
    private Instant credentialsUpdatedAt;

    // Aceite dos Termos de Uso e da Política de Privacidade. Nulos em contas anteriores ao aceite.
    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "terms_version", length = 40)
    private String termsVersion;

    @Column(name = "privacy_version", length = 40)
    private String privacyVersion;

    public AppUser(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public void updateProfile(
            String name,
            String email,
            String username,
            String phone,
            LocalDate birthDate,
            Gender gender,
            String location
    ) {
        this.name = name;
        this.email = email;
        this.username = username;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.location = location;
    }

    public void acceptTerms(Instant acceptedAt, String termsVersion, String privacyVersion) {
        this.termsAcceptedAt = acceptedAt;
        this.termsVersion = termsVersion;
        this.privacyVersion = privacyVersion;
    }

    public void markProfileUpdated() {
        updatedAt = Instant.now();
    }

    // O JWT guarda a emissão em segundos inteiros, então um token emitido no mesmo segundo da troca
    // seria indistinguível do token novo. A troca fica registrada no segundo inteiro seguinte e o
    // token novo é emitido exatamente nesse instante: tudo o que foi emitido antes deixa de valer.
    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        markCredentialsChanged();
    }

    // Troca de e-mail (o login) também invalida os access tokens emitidos antes.
    public void markCredentialsChanged() {
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
