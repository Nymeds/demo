package studdy.example.demo.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
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

    public void markProfileUpdated() {
        updatedAt = Instant.now();
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
