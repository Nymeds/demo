package studdy.example.demo.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Getter
@Entity
@Table(name = "user_profile_photos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfilePhoto {

    public static final int MAX_FILE_SIZE = 2 * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(nullable = false, length = MAX_FILE_SIZE)
    private byte[] content;

    @Column(name = "content_type", nullable = false, length = 16)
    private String contentType;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public UserProfilePhoto(AppUser user, byte[] content, String contentType) {
        this.user = user;
        update(content, contentType);
    }

    public void update(byte[] content, String contentType) {
        this.content = Arrays.copyOf(content, content.length);
        this.contentType = contentType;
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
