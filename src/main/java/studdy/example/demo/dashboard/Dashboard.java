package studdy.example.demo.dashboard;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import studdy.example.demo.user.AppUser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(name = "dashboards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dashboard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DashboardStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @ElementCollection
    @CollectionTable(name = "dashboard_disciplines", joinColumns = @JoinColumn(name = "dashboard_id"))
    @Column(name = "discipline_name", nullable = false, length = 120)
    @OrderColumn(name = "position")
    private List<String> disciplines = new ArrayList<>();

    public Dashboard(String name, DashboardStatus status, AppUser owner, List<String> disciplines) {
        this.name = name;
        this.status = status;
        this.owner = owner;
        this.disciplines = new ArrayList<>(disciplines);
    }
}
