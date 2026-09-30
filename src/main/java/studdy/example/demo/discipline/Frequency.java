package studdy.example.demo.discipline;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "frequency", uniqueConstraints = @UniqueConstraint(name = "uk_frequency_discipline_id", columnNames = "discipline_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Frequency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discipline_id", nullable = false)
    private Discipline discipline;

    @Column(nullable = false)
    private Integer absences;

    // Anuláveis (ao contrário das outras entidades): com ddl-auto=update, as linhas que já
    // existem não têm valor para as colunas novas e um NOT NULL faria o ALTER TABLE falhar.
    @Column(updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    public Frequency(
            Discipline discipline,
            Integer absences
    ) {
        this.discipline = discipline;
        this.absences = absences;
    }

    public void update(
            Integer absences
    ) {
        this.absences = absences;
    }

    public BigDecimal attendancePercentage() {
        return BigDecimal.valueOf(100)
                .subtract(BigDecimal.valueOf(absences).multiply(FrequencyRules.LOSS_PER_ABSENCE))
                .max(BigDecimal.ZERO)
                .setScale(2);
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
