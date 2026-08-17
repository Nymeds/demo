package studdy.example.demo.discipline;

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

    @Column(name = "total_classes", nullable = false)
    private Integer totalClasses;

    @Column(nullable = false)
    private Integer absences;

    public Frequency(
            Discipline discipline,
            Integer totalClasses,
            Integer absences
    ) {
        this.discipline = discipline;
        this.totalClasses = totalClasses;
        this.absences = absences;
    }

    public void update(
            Integer totalClasses,
            Integer absences
    ) {
        this.totalClasses = totalClasses;
        this.absences = absences;
    }
}