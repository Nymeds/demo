package studdy.example.demo.activities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import studdy.example.demo.activities.dto.ActivityResponse;
import studdy.example.demo.activities.dto.CreateActivityRequest;
import studdy.example.demo.activities.dto.UpdateActivityRequest;

import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;

import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineRepository;

import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ActivityServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private ActivityService activityService;

    private AppUser owner;
    private AppUser intruder;

    private Dashboard ownerDashboard;
    private Dashboard intruderDashboard;

    private Discipline ownerDiscipline;
    private Discipline secondOwnerDiscipline;
    private Discipline intruderDiscipline;

    @BeforeEach
    void setUp() {

        owner = userRepository.save(
                new AppUser(
                        "Dono",
                        "atividade-dono@example.com",
                        "hash"
                )
        );

        intruder = userRepository.save(
                new AppUser(
                        "Intruso",
                        "atividade-intruso@example.com",
                        "hash"
                )
        );

        ownerDashboard = dashboardRepository.save(
                new Dashboard(
                        "Semestre do dono",
                        DashboardStatus.ACTIVE,
                        owner
                )
        );

        intruderDashboard = dashboardRepository.save(
                new Dashboard(
                        "Semestre do intruso",
                        DashboardStatus.ACTIVE,
                        intruder
                )
        );

        ownerDiscipline = newDiscipline(
                "Engenharia de Software",
                ownerDashboard
        );

        secondOwnerDiscipline = newDiscipline(
                "Interação Humano-Computador",
                ownerDashboard
        );

        intruderDiscipline = newDiscipline(
                "Banco de Dados",
                intruderDashboard
        );
    }

    @Test
    void rejectsCreatingActivityInDisciplineFromAnotherDashboard() {

        assertNotFound(() ->
                activityService.create(
                        owner.getId(),
                        ownerDashboard.getId(),
                        intruderDiscipline.getId(),
                        new CreateActivityRequest(
                                "Trabalho",
                                "Descrição",
                                LocalDate.of(2026, 9, 10),
                                ActivityStatus.PENDING
                        )
                )
        );
    }

    @Test
    void hidesActivityFromAnotherUserOnGet() {

        ActivityResponse activity = createIntruderActivity();

        assertNotFound(() ->
                activityService.findById(
                        owner.getId(),
                        intruderDashboard.getId(),
                        intruderDiscipline.getId(),
                        activity.id()
                )
        );
    }

    @Test
    void hidesActivityFromAnotherUserOnUpdate() {

        ActivityResponse activity = createIntruderActivity();

        assertNotFound(() ->
                activityService.update(
                        owner.getId(),
                        intruderDashboard.getId(),
                        intruderDiscipline.getId(),
                        activity.id(),
                        new UpdateActivityRequest(
                                "Alterada",
                                "Nova descrição",
                                LocalDate.of(2026, 9, 20),
                                ActivityStatus.IN_PROGRESS
                        )
                )
        );
    }

    @Test
    void hidesActivityFromAnotherUserOnDelete() {

        ActivityResponse activity = createIntruderActivity();

        assertNotFound(() ->
                activityService.delete(
                        owner.getId(),
                        intruderDashboard.getId(),
                        intruderDiscipline.getId(),
                        activity.id()
                )
        );
    }

    @Test
    void findAllOnlyReturnsActivitiesFromRequestedDiscipline() {

        createOwnerActivity(
                ownerDiscipline,
                "Atividade Engenharia"
        );

        createOwnerActivity(
                secondOwnerDiscipline,
                "Atividade IHC"
        );

        List<ActivityResponse> activities = activityService.findAll(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId()
        );

        assertEquals(1, activities.size());
        assertEquals(
                "Atividade Engenharia",
                activities.getFirst().title()
        );
        assertEquals(
                ownerDiscipline.getId(),
                activities.getFirst().disciplineId()
        );
    }

    @Test
    void trimsTitleAndConvertsBlankDescriptionToNull() {

        ActivityResponse response = activityService.create(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId(),
                new CreateActivityRequest(
                        "   Trabalho de Software   ",
                        "      ",
                        LocalDate.of(2026, 9, 15),
                        ActivityStatus.PENDING
                )
        );

        assertEquals(
                "Trabalho de Software",
                response.title()
        );

        assertNull(response.description());
    }

    private ActivityResponse createOwnerActivity(
            Discipline discipline,
            String title
    ) {

        return activityService.create(
                owner.getId(),
                ownerDashboard.getId(),
                discipline.getId(),
                new CreateActivityRequest(
                        title,
                        "Descrição",
                        LocalDate.of(2026, 9, 10),
                        ActivityStatus.PENDING
                )
        );
    }

    private ActivityResponse createIntruderActivity() {

        return activityService.create(
                intruder.getId(),
                intruderDashboard.getId(),
                intruderDiscipline.getId(),
                new CreateActivityRequest(
                        "Atividade privada",
                        "Atividade do outro usuário",
                        LocalDate.of(2026, 9, 10),
                        ActivityStatus.PENDING
                )
        );
    }

    private Discipline newDiscipline(
            String name,
            Dashboard dashboard
    ) {

        return disciplineRepository.save(
                new Discipline(
                        name,
                        "Professor Teste",
                        "#4F46E5",
                        new BigDecimal("6.00"),
                        new BigDecimal("75.00"),
                        dashboard,
                        List.of()
                )
        );
    }

    private void assertNotFound(Runnable access) {

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                access::run
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );
    }
}
