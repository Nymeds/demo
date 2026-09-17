package studdy.example.demo.avatar;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import studdy.example.demo.user.AppUser;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "user_avatars")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAvatar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    // A foto fica no banco, como binário grande, numa tabela própria: assim ela não é carregada
    // junto com o usuário em toda requisição autenticada e nunca fica nos arquivos do frontend.
    // O tipo é fixado para o PostgreSQL gravar em bytea, e não como Large Object (oid), que fica
    // órfão no banco quando a linha é apagada.
    @JdbcTypeCode(SqlTypes.LONG32VARBINARY)
    @Column(name = "content", nullable = false)
    private byte[] content;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserAvatar(AppUser user, byte[] content, String contentType) {
        this.user = user;
        this.content = content.clone();
        this.contentType = contentType;
    }

    public void replace(byte[] content, String contentType) {
        this.content = content.clone();
        this.contentType = contentType;
    }

    // Cópia, para ninguém alterar a imagem guardada na entidade por fora.
    public byte[] getContent() {
        return content.clone();
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
