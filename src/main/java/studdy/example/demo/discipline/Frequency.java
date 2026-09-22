package studdy.example.demo.discipline;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Frequency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "discipline_id", nullable = false)
    private Discipline discipline;

    // Compatibilidade com bancos existentes: esta coluna não participa dos cálculos.
    @Column(name = "total_classes", nullable = false)
    @Getter(AccessLevel.NONE)
    private Integer legacyTotalClasses = 0;

    @Column(nullable = false)
    private Integer absences;

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
}
