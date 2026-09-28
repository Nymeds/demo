package studdy.example.demo.discipline;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.discipline.dto.CreateDisciplineRequest;
import studdy.example.demo.discipline.dto.UpdateDisciplineRequest;
import studdy.example.demo.discipline.dto.ClassScheduleRequest;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class DisciplinePeriodServiceTest {

    @Autowired private UserRepository userRepository;
    @Autowired private DashboardRepository dashboardRepository;
    @Autowired private DisciplineRepository disciplineRepository;
    @Autowired private DisciplineService disciplineService;
    @Autowired private EntityManager entityManager;

    @Test
    void updatesAndPersistsYearAndSemester() {
        AppUser user = userRepository.save(new AppUser("Estudante", "periodo@example.com", "hash"));
        Dashboard dashboard = dashboardRepository.save(new Dashboard("Meu semestre", DashboardStatus.ACTIVE, user));
        List<ClassScheduleRequest> schedules = List.of(new ClassScheduleRequest(
                DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)));
        var created = disciplineService.create(user.getId(), dashboard.getId(), new CreateDisciplineRequest(
                "Cálculo", "Ana", "#4F46E5", new BigDecimal("6.00"), new BigDecimal("75.00"),
                schedules, "1", "2026"));

        var updated = disciplineService.update(user.getId(), dashboard.getId(), created.id(),
                new UpdateDisciplineRequest("Cálculo", "Ana", "#4F46E5", new BigDecimal("6.00"),
                        new BigDecimal("75.00"), schedules, "2", "2027"));
        assertEquals("2027", updated.periodo());
        assertEquals("2", updated.semester());

        entityManager.flush();
        entityManager.clear();
        Discipline reloaded = disciplineRepository.findById(created.id()).orElseThrow();
        assertEquals("2027", reloaded.getPeriodo());
        assertEquals("2", reloaded.getSemester());
    }
}
