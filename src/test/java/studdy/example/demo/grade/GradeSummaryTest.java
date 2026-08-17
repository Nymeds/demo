package studdy.example.demo.grade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.discipline.DisciplineStatus;
import studdy.example.demo.grade.dto.GradeSummaryResponse;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@Transactional
class GradeSummaryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private GradeService gradeService;

    private AppUser user;
    private Dashboard dashboard;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new AppUser("Estudante", "resumo@example.com", "hash"));
        dashboard = dashboardRepository.save(new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, user));
    }

    @Test
    void appliesThePassingAverageDefinedByEachDiscipline() {
        Discipline lenient = newDiscipline("Cálculo", new BigDecimal("6.00"));
        Discipline strict = newDiscipline("Física", new BigDecimal("8.00"));
        gradeRepository.save(newGrade(lenient, new BigDecimal("7.00")));
        gradeRepository.save(newGrade(strict, new BigDecimal("7.00")));

        GradeSummaryResponse lenientSummary = summaryOf(lenient);
        GradeSummaryResponse strictSummary = summaryOf(strict);

        // Mesma nota, situações opostas: é a matéria que decide o critério.
        assertEquals(new BigDecimal("6.00"), lenientSummary.passingAverage());
        assertEquals(DisciplineStatus.APPROVED, lenientSummary.academicSituation());
        assertEquals(new BigDecimal("8.00"), strictSummary.passingAverage());
        assertEquals(DisciplineStatus.FAILED_BY_GRADE, strictSummary.academicSituation());
    }

    @Test
    void reportsNoDataWhileTheDisciplineHasNoGrades() {
        GradeSummaryResponse summary = summaryOf(newDiscipline("Cálculo", new BigDecimal("6.00")));

        assertEquals(0, summary.recordedGrades());
        assertNull(summary.average());
        assertEquals(new BigDecimal("6.00"), summary.passingAverage());
        assertEquals(DisciplineStatus.NO_DATA, summary.academicSituation());
    }

    @Test
    void averagesEveryRecordedGradeOfTheDiscipline() {
        Discipline discipline = newDiscipline("Cálculo", new BigDecimal("6.00"));
        gradeRepository.save(newGrade(discipline, new BigDecimal("5.00")));
        gradeRepository.save(newGrade(discipline, new BigDecimal("8.00")));

        GradeSummaryResponse summary = summaryOf(discipline);

        assertEquals(2, summary.recordedGrades());
        assertEquals(new BigDecimal("6.50"), summary.average());
        assertEquals(DisciplineStatus.APPROVED, summary.academicSituation());
    }

    @Test
    void followsThePassingAverageAfterTheDisciplineIsUpdated() {
        Discipline discipline = newDiscipline("Cálculo", new BigDecimal("6.00"));
        gradeRepository.save(newGrade(discipline, new BigDecimal("7.00")));

        assertEquals(DisciplineStatus.APPROVED, summaryOf(discipline).academicSituation());

        discipline.update("Cálculo", "Professora Ana", 60, new BigDecimal("9.00"), List.of());

        assertEquals(DisciplineStatus.FAILED_BY_GRADE, summaryOf(discipline).academicSituation());
    }

    private GradeSummaryResponse summaryOf(Discipline discipline) {
        return gradeService.summary(user.getId(), dashboard.getId(), discipline.getId());
    }

    private Discipline newDiscipline(String name, BigDecimal passingAverage) {
        return disciplineRepository.save(
                new Discipline(name, "Professora Ana", 60, passingAverage, dashboard, List.of())
        );
    }

    private Grade newGrade(Discipline discipline, BigDecimal score) {
        return new Grade(discipline, "Prova 1", score, LocalDate.of(2026, 8, 1));
    }
}
