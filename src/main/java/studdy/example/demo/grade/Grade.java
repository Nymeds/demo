package studdy.example.demo.grade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import studdy.example.demo.discipline.Discipline;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "grades")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discipline_id", nullable = false)
    private Discipline discipline;

    @Column(name = "assessment_name", nullable = false, length = 120)
    private String assessmentName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal score;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal weight;

    @Column(name = "recorded_at", nullable = false)
    private LocalDate recordedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Grade(
            Discipline discipline,
            String assessmentName,
            BigDecimal score,
            BigDecimal weight,
            LocalDate recordedAt
    ) {
        this.discipline = discipline;
        this.assessmentName = assessmentName;
        this.score = score;
        this.weight = weight;
        this.recordedAt = recordedAt;
    }

    public void update(
            String assessmentName,
            BigDecimal score,
            BigDecimal weight,
            LocalDate recordedAt
    ) {
        this.assessmentName = assessmentName;
        this.score = score;
        this.weight = weight;
        this.recordedAt = recordedAt;
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
