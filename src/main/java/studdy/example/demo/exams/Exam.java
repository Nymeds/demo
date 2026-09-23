
package studdy.example.demo.exams;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import studdy.example.demo.discipline.Discipline;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Entity
@Table(
    name = "exams",
    indexes = {
        @Index(
            name = "idx_exams_discipline_date",
            columnList = "discipline_id, exam_date"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExamType type;

    @Column(name = "exam_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(length = 2000)
    private String content;

    @Column(
        name = "weight_label",
        nullable = false,
        length = 20
    )
    private String weightLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExamStatus status;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "discipline_id",
        nullable = false
    )
    private Discipline discipline;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Exam(
        String title,
        ExamType type,
        LocalDate date,
        LocalTime startTime,
        String content,
        String weightLabel,
        ExamStatus status,
        Discipline discipline
    ) {

        update(
            title,
            type,
            date,
            startTime,
            content,
            weightLabel,
            status,
            discipline
        );
    }

    public void update(
        String title,
        ExamType type,
        LocalDate date,
        LocalTime startTime,
        String content,
        String weightLabel,
        ExamStatus status,
        Discipline discipline
    ) {

        this.title = title;
        this.type = type;
        this.date = date;
        this.startTime = startTime;
        this.content = content;
        this.weightLabel = weightLabel;
        this.status = status;
        this.discipline = discipline;
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