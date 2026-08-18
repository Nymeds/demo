package studdy.example.demo.calendar;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.calendar.dto.CalendarEventResponse;
import studdy.example.demo.calendar.dto.CreateCalendarEventRequest;
import studdy.example.demo.calendar.dto.UpdateCalendarEventRequest;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.discipline.DisciplineService;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Apagar uma disciplina não pode levar junto os eventos do calendário: o evento continua
// na agenda e passa a avisar que a disciplina não existe mais.
@SpringBootTest
@Transactional
class CalendarEventDisciplineDeletionTest {

    private static final String DELETED_DISCIPLINE_NAME = "Essa disciplina não existe mais.";

    private static final LocalDateTime MONDAY = LocalDateTime.of(2026, 9, 14, 8, 0);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private DisciplineService disciplineService;

    @Autowired
    private CalendarEventService calendarEventService;

    @Autowired
    private EntityManager entityManager;

    private AppUser owner;
    private Dashboard dashboard;
    private Discipline discipline;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new AppUser("Dono", "calendario-exclusao@example.com", "hash"));
        dashboard = dashboardRepository.save(new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, owner));
        discipline = disciplineRepository.save(new Discipline(
                "Cálculo",
                "Professora Ana",
                60,
                new BigDecimal("6.00"),
                new BigDecimal("75.00"),
                dashboard,
                List.of()
        ));
    }

    @Test
    void keepsTheEventWhenItsDisciplineIsDeleted() {
        CalendarEventResponse created = createLinkedEvent("Prova 1");

        deleteTheDiscipline();

        CalendarEventResponse found = calendarEventService.findById(
                owner.getId(),
                dashboard.getId(),
                created.id()
        );

        assertEquals("Prova 1", found.title());
        assertNull(found.disciplineId());
        assertTrue(found.disciplineDeleted());
        assertEquals(DELETED_DISCIPLINE_NAME, found.disciplineName());
    }

    @Test
    void stillListsTheEventOfADeletedDisciplineInThePeriod() {
        createLinkedEvent("Aula de Cálculo");

        deleteTheDiscipline();

        List<CalendarEventResponse> week = calendarEventService.findByPeriod(
                owner.getId(),
                dashboard.getId(),
                MONDAY.minusDays(1),
                MONDAY.plusDays(6),
                null
        );

        assertEquals(1, week.size());
        assertTrue(week.getFirst().disciplineDeleted());
        assertEquals(DELETED_DISCIPLINE_NAME, week.getFirst().disciplineName());
    }

    @Test
    void doesNotMarkAStandaloneEventAsDeleted() {
        CalendarEventResponse response = calendarEventService.create(
                owner.getId(),
                dashboard.getId(),
                new CreateCalendarEventRequest(
                        "Consulta médica",
                        null,
                        CalendarEventCategory.OTHER,
                        MONDAY,
                        null,
                        null
                )
        );

        assertFalse(response.disciplineDeleted());
        assertNull(response.disciplineName());
    }

    @Test
    void doesNotMarkAnEventWhoseDisciplineStillExists() {
        CalendarEventResponse response = createLinkedEvent("Prova 1");

        assertFalse(response.disciplineDeleted());
        assertEquals("Cálculo", response.disciplineName());
    }

    @Test
    void stopsWarningWhenTheEventIsEditedWithoutADiscipline() {
        CalendarEventResponse created = createLinkedEvent("Prova 1");

        deleteTheDiscipline();

        CalendarEventResponse updated = calendarEventService.update(
                owner.getId(),
                dashboard.getId(),
                created.id(),
                new UpdateCalendarEventRequest(
                        "Prova 1",
                        null,
                        CalendarEventCategory.EXAM,
                        MONDAY,
                        null,
                        null
                )
        );

        assertFalse(updated.disciplineDeleted());
        assertNull(updated.disciplineName());
    }

    private CalendarEventResponse createLinkedEvent(String title) {
        return calendarEventService.create(
                owner.getId(),
                dashboard.getId(),
                new CreateCalendarEventRequest(
                        title,
                        null,
                        CalendarEventCategory.EXAM,
                        MONDAY,
                        MONDAY.plusHours(2),
                        discipline.getId()
                )
        );
    }

    // Criar o evento e excluir a disciplina acontecem em requisições (transações) diferentes
    // na vida real. Limpar o contexto antes e depois reproduz isso: a exclusão vai ao banco
    // de verdade e o evento é relido de lá, com o vínculo já zerado pelo ON DELETE SET NULL.
    private void deleteTheDiscipline() {
        entityManager.flush();
        entityManager.clear();

        disciplineService.delete(owner.getId(), dashboard.getId(), discipline.getId());

        entityManager.flush();
        entityManager.clear();
    }
}
