package studdy.example.demo.grade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import studdy.example.demo.activities.Activity;
import studdy.example.demo.discipline.Discipline;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
// Índice na chave da disciplina: a lista de notas e a tela Notas agrupam por ela.
@Table(name = "grades", indexes = @Index(name = "idx_grades_discipline_id", columnList = "discipline_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discipline_id", nullable = false)
    private Discipline discipline;

    // Prova ou trabalho cadastrado em Atividades que comprova de onde a nota veio (relatório: RF06).
    // É opcional para as notas antigas. Cada atividade recebe no máximo uma nota, e excluir a
    // atividade mantém a nota, só sem o vínculo.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", unique = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Activity activity;

    @Column(name = "assessment_name", nullable = false, length = 120)
    private String assessmentName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal score;

    @Column(length = 200)
    private String observation;

    public void updateObservation(String observation) {
        this.observation = observation == null || observation.isBlank() ? null : observation.trim();
    }

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
            LocalDate recordedAt
    ) {
        this.discipline = discipline;
        this.assessmentName = assessmentName;
        this.score = score;
        this.recordedAt = recordedAt;
    }

    public Grade(
            Discipline discipline,
            String assessmentName,
            BigDecimal score,
            LocalDate recordedAt,
            Activity activity
    ) {
        this(discipline, assessmentName, score, recordedAt);
        this.activity = activity;
    }

    public void update(
            String assessmentName,
            BigDecimal score,
            LocalDate recordedAt
    ) {
        this.assessmentName = assessmentName;
        this.score = score;
        this.recordedAt = recordedAt;
    }

    public void update(
            String assessmentName,
            BigDecimal score,
            LocalDate recordedAt,
            Activity activity
    ) {
        update(assessmentName, score, recordedAt);
        this.activity = activity;
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
