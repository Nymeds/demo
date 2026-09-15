package studdy.example.demo.gradebook;

import jakarta.persistence.EntityManager;
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
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.grade.Grade;
import studdy.example.demo.grade.GradeRepository;
import studdy.example.demo.gradebook.dto.GradebookEntryResponse;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class GradebookServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private GradebookService gradebookService;

    @Autowired
    private EntityManager entityManager;

    private AppUser owner;
    private Dashboard dashboard;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new AppUser("Estudante", "boletim@example.com", "hash"));
        dashboard = dashboardRepository.save(new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, owner));
    }

    @Test
    void summarizesEveryDisciplineInAlphabeticalOrder() {
        Discipline structures = newDiscipline(dashboard, "Estruturas de Dados", "7.00");
        Discipline databases = newDiscipline(dashboard, "Banco de Dados", "7.00");
        Discipline networks = newDiscipline(dashboard, "Redes de Computadores", "6.00");
        addGrades(structures, "9.50", "9.00");
        addGrades(databases, "6.00", "7.00");

        List<GradebookEntryResponse> entries = summarize();

        assertEquals(List.of("Banco de Dados", "Estruturas de Dados", "Redes de Computadores"),
                entries.stream().map(GradebookEntryResponse::name).toList());

        GradebookEntryResponse databasesEntry = entries.get(0);
        assertEquals(new BigDecimal("6.50"), databasesEntry.average());
        assertEquals(2, databasesEntry.gradeCount());
        assertEquals(PerformanceBand.REGULAR, databasesEntry.band());
        assertFalse(databasesEntry.passing(), "6,50 fica abaixo da média de aprovação 7,00");

        GradebookEntryResponse structuresEntry = entries.get(1);
        assertEquals(new BigDecimal("9.25"), structuresEntry.average());
        assertEquals(PerformanceBand.EXCELLENT, structuresEntry.band());
        assertTrue(structuresEntry.passing());

        GradebookEntryResponse networksEntry = entries.get(2);
        assertEquals(networks.getId(), networksEntry.disciplineId());
        assertNull(networksEntry.average());
        assertEquals(0, networksEntry.gradeCount());
        assertEquals(PerformanceBand.NO_GRADES, networksEntry.band());
    }

    @Test
    void roundsTheAverageLikeTheDisciplineScreen() {
        Discipline discipline = newDiscipline(dashboard, "Cálculo", "6.00");
        addGrades(discipline, "7.00", "8.00", "8.00");

        GradebookEntryResponse entry = summarize().getFirst();

        assertEquals(new BigDecimal("7.67"), entry.average());
        assertEquals(LocalDate.of(2026, 8, 3), entry.lastRecordedAt());
    }

    @Test
    void onlyIncludesTheDisciplinesOfTheRequestedDashboard() {
        Dashboard otherDashboard = dashboardRepository.save(new Dashboard("Semestre 2026.1", DashboardStatus.ACTIVE, owner));
        newDiscipline(dashboard, "Física", "6.00");
        newDiscipline(otherDashboard, "Química", "6.00");

        assertEquals(List.of("Física"), summarize().stream().map(GradebookEntryResponse::name).toList());
    }

    @Test
    void hidesTheDashboardOfAnotherUser() {
        AppUser intruder = userRepository.save(new AppUser("Intruso", "intruso-boletim@example.com", "hash"));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> gradebookService.findAll(intruder.getId(), dashboard.getId())
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    private List<GradebookEntryResponse> summarize() {
        entityManager.flush();
        entityManager.clear();
        return gradebookService.findAll(owner.getId(), dashboard.getId());
    }

    private Discipline newDiscipline(Dashboard target, String name, String passingAverage) {
        return disciplineRepository.save(new Discipline(
                name,
                "Profa. Ana",
                "#4F46E5",
                new BigDecimal(passingAverage),
                new BigDecimal("75.00"),
                target,
                List.of(),
                "2026.2",
                "2"
        ));
    }

    private void addGrades(Discipline discipline, String... scores) {
        for (int index = 0; index < scores.length; index++) {
            gradeRepository.save(new Grade(
                    discipline,
                    "Avaliação " + (index + 1),
                    new BigDecimal(scores[index]),
                    LocalDate.of(2026, 8, index + 1)
            ));
        }
    }
}
