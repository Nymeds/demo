package studdy.example.demo.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import studdy.example.demo.dashboard.dto.CreateDashboardRequest;
import studdy.example.demo.dashboard.dto.DashboardResponse;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class DashboardServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DashboardService dashboardService;

    private AppUser owner;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(
                new AppUser(
                        "Dono",
                        "dashboard-dono@example.com",
                        "hash"
                )
        );
    }

    @Test
    void deactivatesThePreviousActiveDashboardWhenANewOneIsCreatedAsActive() {
        DashboardResponse first = dashboardService.create(
                owner.getId(),
                new CreateDashboardRequest("Semestre 2026.1", DashboardStatus.ACTIVE)
        );

        DashboardResponse second = dashboardService.create(
                owner.getId(),
                new CreateDashboardRequest("Semestre 2026.2", DashboardStatus.ACTIVE)
        );

        Dashboard firstDashboard = dashboardRepository.findById(first.id()).orElseThrow();
        Dashboard secondDashboard = dashboardRepository.findById(second.id()).orElseThrow();

        assertEquals(DashboardStatus.INACTIVE, firstDashboard.getStatus());
        assertEquals(DashboardStatus.ACTIVE, secondDashboard.getStatus());
        assertEquals(DashboardStatus.ACTIVE, second.status());
    }

    @Test
    void neverLeavesMoreThanOneActiveDashboardForTheSameOwner() {
        dashboardService.create(owner.getId(), new CreateDashboardRequest("Semestre 1", DashboardStatus.ACTIVE));
        dashboardService.create(owner.getId(), new CreateDashboardRequest("Semestre 2", DashboardStatus.ACTIVE));
        dashboardService.create(owner.getId(), new CreateDashboardRequest("Semestre 3", DashboardStatus.ACTIVE));

        List<Dashboard> activeDashboards = dashboardRepository.findAllByOwner_IdOrderByNameAsc(owner.getId())
                .stream()
                .filter(dashboard -> dashboard.getStatus() == DashboardStatus.ACTIVE)
                .toList();

        assertEquals(1, activeDashboards.size());
        assertEquals("Semestre 3", activeDashboards.getFirst().getName());
    }

    @Test
    void doesNotAffectOtherDashboardsWhenCreatingAnInactiveOne() {
        DashboardResponse active = dashboardService.create(
                owner.getId(),
                new CreateDashboardRequest("Semestre ativo", DashboardStatus.ACTIVE)
        );

        dashboardService.create(
                owner.getId(),
                new CreateDashboardRequest("Semestre inativo", DashboardStatus.INACTIVE)
        );

        Dashboard activeDashboard = dashboardRepository.findById(active.id()).orElseThrow();

        assertEquals(DashboardStatus.ACTIVE, activeDashboard.getStatus());
    }

    @Test
    void doesNotDeactivateActiveDashboardsFromAnotherOwner() {
        AppUser otherOwner = userRepository.save(
                new AppUser(
                        "Outro dono",
                        "dashboard-outro-dono@example.com",
                        "hash"
                )
        );

        DashboardResponse otherOwnerDashboard = dashboardService.create(
                otherOwner.getId(),
                new CreateDashboardRequest("Semestre do outro dono", DashboardStatus.ACTIVE)
        );

        dashboardService.create(
                owner.getId(),
                new CreateDashboardRequest("Semestre do dono", DashboardStatus.ACTIVE)
        );

        Dashboard otherOwnerDashboardEntity = dashboardRepository.findById(otherOwnerDashboard.id()).orElseThrow();

        assertEquals(DashboardStatus.ACTIVE, otherOwnerDashboardEntity.getStatus());
    }
}
