package studdy.example.demo.discipline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class DisciplineAccessServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private DisciplineAccessService disciplineAccessService;

    private AppUser owner;
    private AppUser intruder;
    private Dashboard dashboard;
    private Discipline discipline;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new AppUser("Dono", "dono@example.com", "hash"));
        intruder = userRepository.save(new AppUser("Intruso", "intruso@example.com", "hash"));
        dashboard = dashboardRepository.save(new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, owner));
        discipline = disciplineRepository.save(
                new Discipline(
                        "Cálculo",
                        "Professora Ana",
                        "#4F46E5",
                        new BigDecimal("6.00"),
                        new BigDecimal("75.0"),
                        dashboard,
                        List.of()
                )
        );
    }

    @Test
    void findsTheDashboardAndTheDisciplineForTheirOwner() {
        assertEquals(dashboard.getId(), disciplineAccessService.findOwnedDashboard(owner.getId(), dashboard.getId()).getId());
        assertEquals(
                discipline.getId(),
                disciplineAccessService.findOwnedDiscipline(owner.getId(), dashboard.getId(), discipline.getId()).getId()
        );
    }

    @Test
    void hidesADashboardThatBelongsToAnotherUser() {
        assertNotFound(() -> disciplineAccessService.findOwnedDashboard(intruder.getId(), dashboard.getId()));
    }

    @Test
    void hidesADisciplineThatBelongsToAnotherUser() {
        assertNotFound(() -> disciplineAccessService.findOwnedDiscipline(
                intruder.getId(),
                dashboard.getId(),
                discipline.getId()
        ));
    }

    @Test
    void hidesADisciplineRequestedThroughTheWrongDashboard() {
        // Mesmo dono, mas a disciplina não pertence a este dashboard.
        Dashboard otherDashboard = dashboardRepository.save(
                new Dashboard("Semestre 2026.1", DashboardStatus.ACTIVE, owner)
        );

        assertNotFound(() -> disciplineAccessService.findOwnedDiscipline(
                owner.getId(),
                otherDashboard.getId(),
                discipline.getId()
        ));
    }

    @Test
    void hidesADisciplineThatDoesNotExist() {
        assertNotFound(() -> disciplineAccessService.findOwnedDiscipline(
                owner.getId(),
                dashboard.getId(),
                UUID.randomUUID()
        ));
    }

    private void assertNotFound(Runnable access) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, access::run);

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }
}
