package studdy.example.demo.gradebook;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

// Teste de performance: a tela Notas precisa de um número fixo de consultas ao banco,
// não importa quantas disciplinas e notas o estudante tenha (sem o problema N+1).
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class GradebookQueryPerformanceTest {

    private static final int DISCIPLINES = 25;
    private static final int GRADES_PER_DISCIPLINE = 6;
    // Uma consulta confere o dono do dashboard e outra agrega as notas de todas as disciplinas.
    private static final long MAX_STATEMENTS = 2;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private EntityManager entityManager;

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

    @Test
    void summarizesManyDisciplinesWithAConstantNumberOfQueries() {
        AppUser owner = userRepository.save(new AppUser("Estudante", "desempenho@example.com", "hash"));
        Dashboard dashboard = dashboardRepository.save(new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, owner));

        for (int disciplineIndex = 0; disciplineIndex < DISCIPLINES; disciplineIndex++) {
            Discipline discipline = disciplineRepository.save(new Discipline(
                    "Disciplina " + disciplineIndex,
                    "Professor " + disciplineIndex,
                    "#4F46E5",
                    new BigDecimal("6.00"),
                    new BigDecimal("75.00"),
                    dashboard,
                    List.of(),
                    "2026.2",
                    "2"
            ));

            for (int gradeIndex = 0; gradeIndex < GRADES_PER_DISCIPLINE; gradeIndex++) {
                gradeRepository.save(new Grade(
                        discipline,
                        "Avaliação " + gradeIndex,
                        new BigDecimal("7.50"),
                        LocalDate.of(2026, 8, gradeIndex + 1)
                ));
            }
        }

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<GradebookEntryResponse> entries = gradebookService.findAll(owner.getId(), dashboard.getId());

        assertEquals(DISCIPLINES, entries.size());
        assertTrue(
                statistics.getPrepareStatementCount() <= MAX_STATEMENTS,
                "a tela Notas usou " + statistics.getPrepareStatementCount() + " consultas para "
                        + DISCIPLINES + " disciplinas; o esperado é no máximo " + MAX_STATEMENTS
        );
    }
}
