package studdy.example.demo.dashboard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import studdy.example.demo.user.AppUser;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "dashboards", indexes = @Index(name = "idx_dashboards_owner_id", columnList = "owner_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dashboard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DashboardStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    // Anuláveis (ao contrário das outras entidades): com ddl-auto=update, as linhas que já
    // existem não têm valor para as colunas novas e um NOT NULL faria o ALTER TABLE falhar.
    @Column(updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    public Dashboard(String name, DashboardStatus status, AppUser owner) {
        this.name = name;
        this.status = status;
        this.owner = owner;
    }

    public void deactivate() {
        this.status = DashboardStatus.INACTIVE;
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
