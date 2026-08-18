package studdy.example.demo.discipline;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.grade.Grade;

@Getter
@Entity
@Table(name = "disciplines")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Discipline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "professor_name", nullable = false, length = 120)
    private String professorName;

    @Column(name = "workload_hours", nullable = false)
    private Integer workloadHours;

    @Column(name = "passing_average", nullable = false, precision = 4, scale = 2)
    private BigDecimal passingAverage;

    @Column(name = "minimum_attendance_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal minimumAttendancePercentage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @ElementCollection
    @CollectionTable(name = "discipline_schedules", joinColumns = @JoinColumn(name = "discipline_id"))
    @OrderColumn(name = "position")
    private List<ClassSchedule> schedules = new ArrayList<>();

    @OneToMany(mappedBy = "discipline", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Grade> grades = new ArrayList<>();
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Discipline(
            String name,
            String professorName,
            Integer workloadHours,
            BigDecimal passingAverage,
            BigDecimal minimumAttendancePercentage,
            Dashboard dashboard,
            List<ClassSchedule> schedules
    ) {
        this.name = name;
        this.professorName = professorName;
        this.workloadHours = workloadHours;
        this.passingAverage = normalizePassingAverage(passingAverage);
        this.minimumAttendancePercentage = normalizeMinimumAttendancePercentage(minimumAttendancePercentage);
        this.dashboard = dashboard;
        this.schedules = new ArrayList<>(schedules);
    }

    public void update(
            String name,
            String professorName,
            Integer workloadHours,
            BigDecimal passingAverage,
            BigDecimal minimumAttendancePercentage,
            List<ClassSchedule> schedules
    ) {
        this.name = name;
        this.professorName = professorName;
        this.workloadHours = workloadHours;
        this.passingAverage = normalizePassingAverage(passingAverage);
        this.minimumAttendancePercentage = normalizeMinimumAttendancePercentage(minimumAttendancePercentage);
        this.schedules.clear();
        this.schedules.addAll(schedules);
        this.updatedAt = Instant.now();
    }

    // As duas medidas são BigDecimal e ficam lado a lado no construtor, então uma troca de
    // ordem compilaria em silêncio. As faixas diferentes (0 a 10 e 0 a 100) são o que faz
    // uma inversão estourar aqui em vez de virar dado errado no banco.
    private static BigDecimal normalizePassingAverage(BigDecimal value) {
        if (value == null
                || value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("A média de aprovação deve estar entre 0 e 10.");
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeMinimumAttendancePercentage(BigDecimal value) {
        if (value == null
                || value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("O percentual mínimo de frequência deve estar entre 0 e 100.");
        }

        return value.setScale(2, RoundingMode.HALF_UP);
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
