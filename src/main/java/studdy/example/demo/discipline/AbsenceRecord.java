package studdy.example.demo.discipline;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "absence_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AbsenceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discipline_id", nullable = false)
    private Discipline discipline;

    @Column(name = "absence_date", nullable = false)
    private LocalDate absenceDate;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 40)
    private String reason;

    @Column(nullable = false, length = 300)
    private String note;

    @Column(name = "impact_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal impactPercentage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public AbsenceRecord(
            Discipline discipline,
            LocalDate absenceDate,
            Integer quantity,
            String reason,
            String note,
            BigDecimal impactPercentage
    ) {
        this.discipline = discipline;
        this.absenceDate = absenceDate;
        this.quantity = quantity;
        this.reason = reason.trim();
        this.note = note == null ? "" : note.trim();
        this.impactPercentage = impactPercentage;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
