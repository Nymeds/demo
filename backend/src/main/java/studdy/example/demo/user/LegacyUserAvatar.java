package studdy.example.demo.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

// Tabela user_avatars do modelo antigo de foto. Existe só para o LegacyAvatarMigrator copiar as
// fotos para user_profile_photos; nenhuma outra parte do sistema grava ou lê esta entidade.
@Getter
@Entity
@Immutable
@Table(name = "user_avatars")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegacyUserAvatar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.LONG32VARBINARY)
    @Column(name = "content", nullable = false)
    private byte[] content;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Só os testes criam linhas do modelo antigo.
    public LegacyUserAvatar(UUID userId, byte[] content, String contentType) {
        this.userId = userId;
        this.content = content.clone();
        this.contentType = contentType;
        this.updatedAt = Instant.now();
    }
}
