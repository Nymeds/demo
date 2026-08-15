package studdy.example.demo.discipline;

import jakarta.persistence.CollectionTable;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import studdy.example.demo.dashboard.Dashboard;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Column(nullable = false, length = 7)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @ElementCollection
    @CollectionTable(name = "discipline_schedules", joinColumns = @JoinColumn(name = "discipline_id"))
    @OrderColumn(name = "position")
    private List<ClassSchedule> schedules = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Discipline(
            String name,
            String professorName,
            String color,
            Dashboard dashboard,
            List<ClassSchedule> schedules
    ) {
        this.name = name;
        this.professorName = professorName;
        this.color = color;
        this.dashboard = dashboard;
        this.schedules = new ArrayList<>(schedules);
    }

    public void update(
            String name,
            String professorName,
            String color,
            List<ClassSchedule> schedules
    ) {
        this.name = name;
        this.professorName = professorName;
        this.color = color;
        this.schedules.clear();
        this.schedules.addAll(schedules);
        this.updatedAt = Instant.now();
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
