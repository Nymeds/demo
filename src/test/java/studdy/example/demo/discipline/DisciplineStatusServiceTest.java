package studdy.example.demo.discipline;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.discipline.dto.DisciplineResponse;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class DisciplineStatusServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private DisciplineService disciplineService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void changesAndPersistsOnlyTheLifecycleStatus() {
        AppUser user = userRepository.save(new AppUser("Estudante", "status@example.com", "hash"));
        Dashboard dashboard = dashboardRepository.save(new Dashboard("Semestre", DashboardStatus.ACTIVE, user));
        Discipline discipline = disciplineRepository.save(new Discipline(
                "Cálculo",
                "Professora Ana",
                "#4F46E5",
                new BigDecimal("6.00"),
                new BigDecimal("75.00"),
                dashboard,
                List.of(),
                "1",
                "2026"
        ));

        DisciplineResponse response = disciplineService.updateStatus(
                user.getId(),
                dashboard.getId(),
                discipline.getId(),
                DisciplineLifecycleStatus.LOCKED
        );

        entityManager.flush();
        entityManager.clear();

        Discipline persisted = disciplineRepository.findById(discipline.getId()).orElseThrow();
        assertEquals(DisciplineLifecycleStatus.LOCKED, response.status());
        assertEquals(DisciplineLifecycleStatus.LOCKED, persisted.getStatus());
        assertEquals("Cálculo", persisted.getName());
    }
}
