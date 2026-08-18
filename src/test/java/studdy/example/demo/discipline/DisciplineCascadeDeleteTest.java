package studdy.example.demo.discipline;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.activities.Activity;
import studdy.example.demo.activities.ActivityRepository;
import studdy.example.demo.activities.ActivityStatus;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.grade.Grade;
import studdy.example.demo.grade.GradeRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class DisciplineCascadeDeleteTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private FrequencyRepository frequencyRepository;

    @Autowired
    private EntityManager entityManager;

    private Dashboard dashboard;

    @BeforeEach
    void setUp() {
        AppUser user = userRepository.save(new AppUser("Estudante", "cascata@example.com", "hash"));
        dashboard = dashboardRepository.save(new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, user));
    }

    @Test
    void deletesEveryChildWhenTheDisciplineIsDeleted() {
        Discipline discipline = newDiscipline("Cálculo");
        UUID gradeId = gradeRepository.save(newGrade(discipline)).getId();
        UUID activityId = activityRepository.save(newActivity(discipline)).getId();
        UUID frequencyId = frequencyRepository.save(new Frequency(discipline, 60, 6)).getId();
        UUID disciplineId = discipline.getId();

        deleteReloaded(disciplineId);

        assertFalse(disciplineRepository.existsById(disciplineId));
        assertFalse(gradeRepository.existsById(gradeId), "a nota ficou órfã");
        assertFalse(activityRepository.existsById(activityId), "a atividade ficou órfã");
        assertFalse(frequencyRepository.existsById(frequencyId), "a frequência ficou órfã");
        assertEquals(0L, countSchedulesOf(disciplineId), "os horários ficaram órfãos");
    }

    @Test
    void deletesADisciplineThatHasNoChildrenAtAll() {
        UUID disciplineId = newDiscipline("Física").getId();

        deleteReloaded(disciplineId);

        assertFalse(disciplineRepository.existsById(disciplineId));
    }

    @Test
    void keepsTheChildrenOfTheOtherDisciplines() {
        Discipline deleted = newDiscipline("Cálculo");
        Discipline kept = newDiscipline("Física");
        gradeRepository.save(newGrade(deleted));
        activityRepository.save(newActivity(deleted));
        frequencyRepository.save(new Frequency(deleted, 60, 6));

        UUID keptGradeId = gradeRepository.save(newGrade(kept)).getId();
        UUID keptActivityId = activityRepository.save(newActivity(kept)).getId();
        UUID keptFrequencyId = frequencyRepository.save(new Frequency(kept, 40, 2)).getId();
        UUID keptDisciplineId = kept.getId();

        deleteReloaded(deleted.getId());

        assertTrue(gradeRepository.existsById(keptGradeId));
        assertTrue(activityRepository.existsById(keptActivityId));
        assertTrue(frequencyRepository.existsById(keptFrequencyId));
        assertEquals(1L, countSchedulesOf(keptDisciplineId));
    }

    // Criar os filhos e apagar a disciplina acontecem em requisições diferentes na vida real,
    // então a disciplina precisa ser recarregada do banco: a instância que ficou em memória
    // nunca soube dos filhos salvos pelos repositórios deles.
    private void deleteReloaded(UUID disciplineId) {
        entityManager.flush();
        entityManager.clear();

        disciplineRepository.delete(disciplineRepository.findById(disciplineId).orElseThrow());
        disciplineRepository.flush();
    }

    private long countSchedulesOf(UUID disciplineId) {
        Object total = entityManager
                .createNativeQuery("select count(*) from discipline_schedules where discipline_id = :disciplineId")
                .setParameter("disciplineId", disciplineId)
                .getSingleResult();

        return ((Number) total).longValue();
    }

    private Discipline newDiscipline(String name) {
        return disciplineRepository.save(new Discipline(
                name,
                "Professora Ana",
                "#4F46E5",
                new BigDecimal("6.00"),
                new BigDecimal("75.0"),
                dashboard,
                List.of(new ClassSchedule(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)))
        ));
    }

    private Grade newGrade(Discipline discipline) {
        return new Grade(discipline, "Prova 1", new BigDecimal("8.00"), LocalDate.of(2026, 8, 15));
    }

    private Activity newActivity(Discipline discipline) {
        return new Activity(
                "Trabalho final",
                "Entrega em dupla",
                LocalDate.of(2026, 9, 1),
                ActivityStatus.PENDING,
                discipline
        );
    }
}
