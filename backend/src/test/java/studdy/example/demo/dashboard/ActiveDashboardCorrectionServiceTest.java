package studdy.example.demo.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class ActiveDashboardCorrectionServiceTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DashboardRepository dashboardRepository;
    @Autowired
    private DisciplineRepository disciplineRepository;
    @Autowired
    private ActiveDashboardCorrectionService correctionService;

    private AppUser user(String email) {
        return userRepository.save(new AppUser("U", email, "hash"));
    }

    private Dashboard dash(AppUser owner, String name, DashboardStatus status) {
        return dashboardRepository.saveAndFlush(new Dashboard(name, status, owner));
    }

    private void addDisciplines(Dashboard dashboard, int n) {
        for (int i = 0; i < n; i++) {
            disciplineRepository.saveAndFlush(new Discipline("D" + i, "Prof", "#fff",
                    new BigDecimal("6"), new BigDecimal("75"), dashboard, List.of(), "2026.1", "MANHA"));
        }
    }

    private List<Dashboard> active(AppUser owner) {
        return dashboardRepository.findAllByOwner_IdAndStatus(owner.getId(), DashboardStatus.ACTIVE);
    }

    @Test
    void keepsOnlyMostRecentActivePerOwnerAndIsIdempotent() {
        AppUser a = user("corr-a@example.com");
        AppUser b = user("corr-b@example.com");
        AppUser c = user("corr-c@example.com");
        List<Dashboard> multi = List.of(dash(a, "1", DashboardStatus.ACTIVE),
                dash(a, "2", DashboardStatus.ACTIVE), dash(a, "3", DashboardStatus.ACTIVE));
        Dashboard expected = multi.stream().min(Comparator.comparing(Dashboard::getId)).orElseThrow();
        addDisciplines(expected, 2);
        multi.stream().filter(m -> m != expected).findFirst().ifPresent(m -> addDisciplines(m, 1));
        Dashboard single = dash(b, "s", DashboardStatus.ACTIVE);
        Dashboard inactive = dash(b, "i", DashboardStatus.INACTIVE);
        dash(c, "x", DashboardStatus.ACTIVE);
        dash(c, "y", DashboardStatus.ACTIVE);

        ActiveDashboardCorrectionService.Result first = correctionService.correct();

        assertEquals(2, first.accountsAdjusted());
        assertEquals(3, first.dashboardsDeactivated());
        assertEquals(List.of(expected.getId()), active(a).stream().map(Dashboard::getId).toList());
        assertEquals(3, dashboardRepository.findAllByOwner_IdOrderByNameAsc(a.getId()).size());
        assertEquals(List.of(single.getId()), active(b).stream().map(Dashboard::getId).toList());
        assertEquals(DashboardStatus.INACTIVE, dashboardRepository.findById(inactive.getId()).orElseThrow().getStatus());
        assertEquals(1, active(c).size());

        ActiveDashboardCorrectionService.Result second = correctionService.correct();

        assertEquals(0, second.accountsAdjusted());
        assertEquals(0, second.dashboardsDeactivated());
        assertEquals(List.of(expected.getId()), active(a).stream().map(Dashboard::getId).toList());
    }
}
