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

import studdy.example.demo.grade.GradeService;
import studdy.example.demo.grade.dto.CreateGradeRequest;
import studdy.example.demo.grade.dto.GradeResponse;

import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Autowired
    private GradeService gradeService;

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
                                ActivityStatus.PENDING,
                                null
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
                                ActivityStatus.IN_PROGRESS,
                                null
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
                ownerDiscipline.getId(),
                null
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
    void findAllByDashboardReturnsActivitiesFromAllDisciplinesOrderedByDueDate() {

        createOwnerActivity(
                ownerDiscipline,
                "Atividade tardia",
                LocalDate.of(2026, 9, 20)
        );

        createOwnerActivity(
                secondOwnerDiscipline,
                "Atividade antecipada",
                LocalDate.of(2026, 9, 5)
        );

        List<ActivityResponse> activities = activityService.findAllByDashboard(
                owner.getId(),
                ownerDashboard.getId(),
                null
        );

        assertEquals(2, activities.size());
        assertEquals("Atividade antecipada", activities.get(0).title());
        assertEquals("Atividade tardia", activities.get(1).title());
    }

    @Test
    void rejectsFindAllByDashboardForAnotherUser() {

        assertNotFound(() ->
                activityService.findAllByDashboard(
                        owner.getId(),
                        intruderDashboard.getId(),
                        null
                )
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
                        ActivityStatus.PENDING,
                        null
                )
        );

        assertEquals(
                "Trabalho de Software",
                response.title()
        );

        assertNull(response.description());
    }

    @Test
    void createWithoutTypeDefaultsToActivity() {

        ActivityResponse response = activityService.create(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId(),
                new CreateActivityRequest(
                        "Trabalho",
                        "Descrição",
                        LocalDate.of(2026, 9, 15),
                        ActivityStatus.PENDING,
                        null
                )
        );

        assertEquals(ActivityType.ACTIVITY, response.type());
    }

    @Test
    void createWithExplicitTypeKeepsIt() {

        ActivityResponse response = activityService.create(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId(),
                new CreateActivityRequest(
                        "Prova 1",
                        "Descrição",
                        LocalDate.of(2026, 9, 15),
                        ActivityStatus.PENDING,
                        ActivityType.EXAM
                )
        );

        assertEquals(ActivityType.EXAM, response.type());
    }

    @Test
    void updateWithoutTypeKeepsTheCurrentType() {

        ActivityResponse created = activityService.create(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId(),
                new CreateActivityRequest(
                        "Prova 1",
                        "Descrição",
                        LocalDate.of(2026, 9, 15),
                        ActivityStatus.PENDING,
                        ActivityType.EXAM
                )
        );

        ActivityResponse updated = activityService.update(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId(),
                created.id(),
                new UpdateActivityRequest(
                        "Prova 1 revisada",
                        "Descrição",
                        LocalDate.of(2026, 9, 16),
                        ActivityStatus.IN_PROGRESS,
                        null
                )
        );

        assertEquals(ActivityType.EXAM, updated.type());
    }

    @Test
    void updateWithTypeChangesIt() {

        ActivityResponse created = activityService.create(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId(),
                new CreateActivityRequest(
                        "Trabalho",
                        "Descrição",
                        LocalDate.of(2026, 9, 15),
                        ActivityStatus.PENDING,
                        null
                )
        );

        ActivityResponse updated = activityService.update(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId(),
                created.id(),
                new UpdateActivityRequest(
                        "Virou prova",
                        "Descrição",
                        LocalDate.of(2026, 9, 16),
                        ActivityStatus.IN_PROGRESS,
                        ActivityType.EXAM
                )
        );

        assertEquals(ActivityType.EXAM, updated.type());
    }

    @Test
    void findAllFiltersByType() {

        activityService.create(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId(),
                new CreateActivityRequest(
                        "Trabalho",
                        "Descrição",
                        LocalDate.of(2026, 9, 10),
                        ActivityStatus.PENDING,
                        ActivityType.ACTIVITY
                )
        );

        activityService.create(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId(),
                new CreateActivityRequest(
                        "Prova",
                        "Descrição",
                        LocalDate.of(2026, 9, 12),
                        ActivityStatus.PENDING,
                        ActivityType.EXAM
                )
        );

        List<ActivityResponse> exams = activityService.findAll(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId(),
                ActivityType.EXAM
        );

        assertEquals(1, exams.size());
        assertEquals("Prova", exams.getFirst().title());

        List<ActivityResponse> all = activityService.findAll(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId(),
                null
        );

        assertEquals(2, all.size());
    }

    @Test
    void findAllByDashboardFiltersByType() {

        activityService.create(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId(),
                new CreateActivityRequest(
                        "Trabalho",
                        "Descrição",
                        LocalDate.of(2026, 9, 10),
                        ActivityStatus.PENDING,
                        ActivityType.ACTIVITY
                )
        );

        activityService.create(
                owner.getId(),
                ownerDashboard.getId(),
                secondOwnerDiscipline.getId(),
                new CreateActivityRequest(
                        "Prova",
                        "Descrição",
                        LocalDate.of(2026, 9, 12),
                        ActivityStatus.PENDING,
                        ActivityType.EXAM
                )
        );

        List<ActivityResponse> exams = activityService.findAllByDashboard(
                owner.getId(),
                ownerDashboard.getId(),
                ActivityType.EXAM
        );

        assertEquals(1, exams.size());
        assertEquals("Prova", exams.getFirst().title());

        List<ActivityResponse> all = activityService.findAllByDashboard(
                owner.getId(),
                ownerDashboard.getId(),
                null
        );

        assertEquals(2, all.size());
    }

    @Test
    void rejectsMovingDueDateAfterLinkedGradeRecordedAt() {

        ActivityResponse activity = createOwnerActivity(
                ownerDiscipline,
                "Prova 1",
                LocalDate.of(2026, 9, 10)
        );

        GradeResponse grade = gradeService.create(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId(),
                new CreateGradeRequest(
                        "Prova 1",
                        new BigDecimal("8.00"),
                        LocalDate.of(2026, 9, 12),
                        activity.id()
                )
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> activityService.update(
                        owner.getId(),
                        ownerDashboard.getId(),
                        ownerDiscipline.getId(),
                        activity.id(),
                        new UpdateActivityRequest(
                                "Prova 1",
                                "Descrição",
                                LocalDate.of(2026, 9, 13),
                                ActivityStatus.PENDING,
                                null
                        )
                )
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(
                "O prazo não pode ficar depois de 12/09/2026, data da nota lançada. Exclua a nota ou ajuste a data dela (até hoje) antes.",
                error.getReason()
        );
        assertNotNull(grade);
    }

    @Test
    void allowsMovingDueDateToSameOrEarlierDateThanLinkedGradeRecordedAt() {

        ActivityResponse activity = createOwnerActivity(
                ownerDiscipline,
                "Prova 1",
                LocalDate.of(2026, 9, 10)
        );

        gradeService.create(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId(),
                new CreateGradeRequest(
                        "Prova 1",
                        new BigDecimal("8.00"),
                        LocalDate.of(2026, 9, 12),
                        activity.id()
                )
        );

        ActivityResponse updated = activityService.update(
                owner.getId(),
                ownerDashboard.getId(),
                ownerDiscipline.getId(),
                activity.id(),
                new UpdateActivityRequest(
                        "Prova 1",
                        "Descrição",
                        LocalDate.of(2026, 9, 12),
                        ActivityStatus.PENDING,
                        null
                )
        );

        assertEquals(LocalDate.of(2026, 9, 12), updated.dueDate());
    }

    private ActivityResponse createOwnerActivity(
            Discipline discipline,
            String title
    ) {
        return createOwnerActivity(discipline, title, LocalDate.of(2026, 9, 10));
    }

    private ActivityResponse createOwnerActivity(
            Discipline discipline,
            String title,
            LocalDate dueDate
    ) {

        return activityService.create(
                owner.getId(),
                ownerDashboard.getId(),
                discipline.getId(),
                new CreateActivityRequest(
                        title,
                        "Descrição",
                        dueDate,
                        ActivityStatus.PENDING,
                        null
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
                        ActivityStatus.PENDING,
                        null
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
                        ,
                        "2023.1",
                        "1"
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
