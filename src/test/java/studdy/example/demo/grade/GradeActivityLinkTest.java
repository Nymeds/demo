package studdy.example.demo.grade;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.activities.Activity;
import studdy.example.demo.activities.ActivityRepository;
import studdy.example.demo.activities.ActivityService;
import studdy.example.demo.activities.ActivityStatus;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.discipline.ClassSchedule;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.grade.dto.CreateGradeRequest;
import studdy.example.demo.grade.dto.GradeResponse;
import studdy.example.demo.grade.dto.UpdateGradeRequest;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class GradeActivityLinkTest {

    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private GradeService gradeService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private EntityManager entityManager;

    private UUID userId;
    private Dashboard dashboard;
    private Discipline discipline;

    @BeforeEach
    void setUp() {
        AppUser user = userRepository.save(new AppUser("Estudante", "vinculo@example.com", "hash"));
        userId = user.getId();
        dashboard = dashboardRepository.save(new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, user));
        discipline = newDiscipline("Cálculo");
    }

    @Test
    void linksTheGradeToAnActivityOfTheSameDiscipline() {
        Activity exam = newActivity(discipline, TODAY.minusDays(3));

        GradeResponse response = create(exam.getId(), TODAY);

        assertEquals(exam.getId(), response.activityId());
    }

    @Test
    void keepsAcceptingGradesWithoutAnActivity() {
        GradeResponse response = gradeService.create(
                userId,
                dashboard.getId(),
                discipline.getId(),
                new CreateGradeRequest("Prova antiga", new BigDecimal("7.00"), TODAY)
        );

        assertNull(response.activityId());
    }

    @Test
    void rejectsAnActivityFromAnotherDiscipline() {
        Activity otherExam = newActivity(newDiscipline("Física"), TODAY.minusDays(3));

        assertStatus(HttpStatus.NOT_FOUND, () -> create(otherExam.getId(), TODAY));
    }

    @Test
    void rejectsAnActivityThatHasNotHappenedYet() {
        Activity futureExam = newActivity(discipline, TODAY.plusDays(2));

        assertStatus(HttpStatus.BAD_REQUEST, () -> create(futureExam.getId(), TODAY));
    }

    @Test
    void rejectsAGradeDatedBeforeTheActivity() {
        Activity exam = newActivity(discipline, TODAY.minusDays(3));

        assertStatus(HttpStatus.BAD_REQUEST, () -> create(exam.getId(), TODAY.minusDays(5)));
    }

    @Test
    void rejectsASecondGradeForTheSameActivity() {
        Activity exam = newActivity(discipline, TODAY.minusDays(3));
        create(exam.getId(), TODAY);

        assertStatus(HttpStatus.CONFLICT, () -> create(exam.getId(), TODAY));
    }

    @Test
    void updatingAGradeKeepsItsOwnActivity() {
        Activity exam = newActivity(discipline, TODAY.minusDays(3));
        GradeResponse created = create(exam.getId(), TODAY);

        GradeResponse updated = gradeService.update(
                userId,
                dashboard.getId(),
                discipline.getId(),
                created.id(),
                new UpdateGradeRequest("Prova 1", new BigDecimal("9.50"), TODAY, exam.getId())
        );

        assertEquals(exam.getId(), updated.activityId());
        assertEquals(0, new BigDecimal("9.50").compareTo(updated.score()));
    }

    @Test
    void deletingTheActivityKeepsTheGradeWithoutTheLink() {
        Activity exam = newActivity(discipline, TODAY.minusDays(3));
        UUID gradeId = create(exam.getId(), TODAY).id();
        flushAndClear();

        activityService.delete(userId, dashboard.getId(), discipline.getId(), exam.getId());
        flushAndClear();

        assertNull(gradeRepository.findById(gradeId).orElseThrow().getActivity());
    }

    @Test
    void deletingTheDisciplineRemovesTheLinkedGradeAndActivity() {
        Activity exam = newActivity(discipline, TODAY.minusDays(3));
        UUID gradeId = create(exam.getId(), TODAY).id();
        UUID activityId = exam.getId();
        flushAndClear();

        disciplineRepository.delete(disciplineRepository.findById(discipline.getId()).orElseThrow());
        disciplineRepository.flush();

        assertFalse(gradeRepository.existsById(gradeId), "a nota ficou órfã");
        assertFalse(activityRepository.existsById(activityId), "a atividade ficou órfã");
    }

    private GradeResponse create(UUID activityId, LocalDate recordedAt) {
        return gradeService.create(
                userId,
                dashboard.getId(),
                discipline.getId(),
                new CreateGradeRequest("Prova 1", new BigDecimal("8.00"), recordedAt, activityId)
        );
    }

    private void assertStatus(HttpStatus expected, Executable action) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, action);
        assertEquals(expected, error.getStatusCode());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private Discipline newDiscipline(String name) {
        return disciplineRepository.save(new Discipline(
                name,
                "Professora Ana",
                "#4F46E5",
                new BigDecimal("6.00"),
                new BigDecimal("75.0"),
                dashboard,
                List.of(new ClassSchedule(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))),
                "2026.2",
                "2"
        ));
    }

    private Activity newActivity(Discipline owner, LocalDate dueDate) {
        return activityRepository.save(new Activity(
                "Prova 1",
                "Capítulos 1 a 3",
                dueDate,
                ActivityStatus.COMPLETED,
                owner
        ));
    }
}
