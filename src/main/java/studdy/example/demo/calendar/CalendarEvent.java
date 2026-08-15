package studdy.example.demo.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.discipline.Discipline;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "calendar_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discipline_id")
    private Discipline discipline;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CalendarEventCategory category;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public CalendarEvent(
            Dashboard dashboard,
            Discipline discipline,
            String title,
            String description,
            CalendarEventCategory category,
            LocalDateTime startsAt,
            LocalDateTime endsAt
    ) {
        this.dashboard = dashboard;
        this.discipline = discipline;
        this.title = title;
        this.description = description;
        this.category = category;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public void update(
            Discipline discipline,
            String title,
            String description,
            CalendarEventCategory category,
            LocalDateTime startsAt,
            LocalDateTime endsAt
    ) {
        this.discipline = discipline;
        this.title = title;
        this.description = description;
        this.category = category;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
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
