package studdy.example.demo.grade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.activities.Activity;
import studdy.example.demo.activities.ActivityRepository;
import studdy.example.demo.activities.ActivityStatus;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.grade.dto.CreateGradeRequest;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

// Cobre o caso de dois envios simultâneos: a checagem prévia do serviço não vê a outra nota,
// e quem impede a duplicidade é a restrição única de grades.activity_id no banco.
@SpringBootTest
@Transactional
class GradeActivityUniqueConstraintTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @MockitoSpyBean
    private GradeRepository gradeRepository;

    @Autowired
    private GradeService gradeService;

    private UUID userId;
    private Dashboard dashboard;
    private Discipline discipline;
    private Activity exam;

    @BeforeEach
    void setUp() {
        AppUser user = userRepository.save(new AppUser("Estudante", "restricao-nota@example.com", "hash"));
        userId = user.getId();
        dashboard = dashboardRepository.save(new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, user));
        discipline = disciplineRepository.save(new Discipline(
                "Cálculo",
                "Professora Ana",
                "#4F46E5",
                new BigDecimal("6.00"),
                new BigDecimal("75.0"),
                dashboard,
                List.of(),
                "2026.2",
                "2"
        ));
        exam = activityRepository.save(new Activity(
                "Prova 1",
                null,
                LocalDate.now().minusDays(3),
                ActivityStatus.COMPLETED,
                discipline
        ));
    }

    @Test
    void theDatabaseRejectsTwoGradesForTheSameActivity() {
        gradeRepository.saveAndFlush(newLinkedGrade());

        assertThrows(DataIntegrityViolationException.class, () -> gradeRepository.saveAndFlush(newLinkedGrade()));
    }

    @Test
    void theServiceTurnsTheDatabaseConflictIntoA409() {
        gradeRepository.saveAndFlush(newLinkedGrade());
        // Simula a corrida: a outra nota ainda não era visível quando o serviço fez a checagem prévia.
        doReturn(false).when(gradeRepository).existsByActivity_Id(any());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> gradeService.create(
                userId,
                dashboard.getId(),
                discipline.getId(),
                new CreateGradeRequest("Prova 1", new BigDecimal("7.00"), LocalDate.now(), exam.getId())
        ));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    private Grade newLinkedGrade() {
        return new Grade(discipline, "Prova 1", new BigDecimal("8.00"), LocalDate.now(), exam);
    }
}
