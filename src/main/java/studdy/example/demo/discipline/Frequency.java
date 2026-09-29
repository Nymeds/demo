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

    @OneToOne
    @JoinColumn(name = "discipline_id", nullable = false)
    private Discipline discipline;

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
