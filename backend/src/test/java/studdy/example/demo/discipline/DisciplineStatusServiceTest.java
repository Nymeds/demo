package studdy.example.demo.discipline;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.discipline.dto.ClassScheduleRequest;
import studdy.example.demo.discipline.dto.CreateDisciplineRequest;
import studdy.example.demo.discipline.dto.DisciplineResponse;
import studdy.example.demo.discipline.dto.UpdateDisciplineRequest;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    void updatesSemesterAndPeriodAndStoresNullForBlankProfessor() {
        AppUser user = userRepository.save(new AppUser("Estudante", "edit@example.com", "hash"));
        Dashboard dashboard = dashboardRepository.save(new Dashboard("Semestre", DashboardStatus.ACTIVE, user));
        Discipline discipline = disciplineRepository.save(new Discipline(
                "Cálculo", "Professora Ana", "#4F46E5", new BigDecimal("6.00"),
                new BigDecimal("75.00"), dashboard, List.of(), "1", "2026"));

        DisciplineResponse response = disciplineService.update(
                user.getId(), dashboard.getId(), discipline.getId(),
                new UpdateDisciplineRequest("Cálculo", "   ", "#4F46E5", new BigDecimal("6.00"),
                        new BigDecimal("75.00"),
                        List.of(new ClassScheduleRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))),
                        "2", "2027"));
        entityManager.flush();
        entityManager.clear();

        Discipline persisted = disciplineRepository.findById(discipline.getId()).orElseThrow();
        assertEquals("2", persisted.getSemester());
        assertEquals("2027", persisted.getPeriodo());
        assertNull(persisted.getProfessorName());
        assertNull(response.professorName());
    }

    @Test
    void createsWithNullProfessorWhenOmitted() {
        AppUser user = userRepository.save(new AppUser("Estudante", "create@example.com", "hash"));
        Dashboard dashboard = dashboardRepository.save(new Dashboard("Semestre", DashboardStatus.ACTIVE, user));

        DisciplineResponse response = disciplineService.create(user.getId(), dashboard.getId(),
                new CreateDisciplineRequest("Física", "", "#4F46E5", new BigDecimal("6.00"),
                        new BigDecimal("75.00"),
                        List.of(new ClassScheduleRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))),
                        "1", "2026"));
        entityManager.flush();
        entityManager.clear();

        assertNull(response.professorName());
        assertNull(disciplineRepository.findById(response.id()).orElseThrow().getProfessorName());
    }
}
