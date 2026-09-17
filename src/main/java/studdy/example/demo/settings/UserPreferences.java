package studdy.example.demo.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import studdy.example.demo.user.AppUser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "user_preferences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreferences {

    public static final int DEFAULT_DEADLINE_ALERT_DAYS = 3;
    public static final int DEFAULT_ATTENDANCE_ALERT_MARGIN = 10;
    public static final StartSection DEFAULT_START_SECTION = StartSection.DASHBOARD;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    // Quantos dias antes do prazo uma atividade passa a gerar aviso.
    @Column(name = "deadline_alert_days", nullable = false)
    private int deadlineAlertDays;

    // Pontos percentuais acima da frequência mínima da disciplina em que o aviso começa.
    // Com mínimo de 75% e margem 10, o estudante é avisado abaixo de 85%.
    @Column(name = "attendance_alert_margin", nullable = false)
    private int attendanceAlertMargin;

    @Enumerated(EnumType.STRING)
    @Column(name = "start_section", nullable = false, length = 20)
    private StartSection startSection;

    // Média que o estudante quer alcançar, de 0 a 10. Opcional: fica vazia até ser definida.
    @Column(name = "grade_goal", precision = 4, scale = 2)
    private BigDecimal gradeGoal;

    @Column(nullable = false)
    private Instant updatedAt;

    public UserPreferences(AppUser user) {
        this.user = user;
        this.deadlineAlertDays = DEFAULT_DEADLINE_ALERT_DAYS;
        this.attendanceAlertMargin = DEFAULT_ATTENDANCE_ALERT_MARGIN;
        this.startSection = DEFAULT_START_SECTION;
    }

    public void update(
            int deadlineAlertDays,
            int attendanceAlertMargin,
            StartSection startSection,
            BigDecimal gradeGoal
    ) {
        this.deadlineAlertDays = deadlineAlertDays;
        this.attendanceAlertMargin = attendanceAlertMargin;
        this.startSection = startSection;
        this.gradeGoal = gradeGoal == null ? null : gradeGoal.setScale(2, RoundingMode.HALF_UP);
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
