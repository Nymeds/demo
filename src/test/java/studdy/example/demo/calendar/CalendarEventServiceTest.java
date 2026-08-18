package studdy.example.demo.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.calendar.dto.CalendarEventResponse;
import studdy.example.demo.calendar.dto.CreateCalendarEventRequest;
import studdy.example.demo.calendar.dto.UpdateCalendarEventRequest;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class CalendarEventServiceTest {

    // Semana fixa no futuro para os testes de intervalo: assim a virada de mês ou o
    // horário em que a suíte roda nunca mudam o resultado.
    private static final LocalDateTime MONDAY = LocalDateTime.of(2026, 9, 14, 8, 0);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private CalendarEventService calendarEventService;

    private AppUser owner;
    private AppUser intruder;
    private Dashboard dashboard;
    private Dashboard intruderDashboard;
    private Discipline discipline;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new AppUser("Dono", "calendario-dono@example.com", "hash"));
        intruder = userRepository.save(new AppUser("Intruso", "calendario-intruso@example.com", "hash"));
        dashboard = dashboardRepository.save(new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, owner));
        intruderDashboard = dashboardRepository.save(
                new Dashboard("Semestre do intruso", DashboardStatus.ACTIVE, intruder)
        );
        discipline = newDiscipline(dashboard, "Cálculo");
    }

    @Test
    void showsTheDisciplineNameOfALinkedEvent() {
        CalendarEventResponse response = create(
                "Aula de Cálculo",
                CalendarEventCategory.CLASS,
                MONDAY,
                MONDAY.plusHours(2),
                discipline.getId()
        );

        assertEquals(discipline.getId(), response.disciplineId());
        assertEquals("Cálculo", response.disciplineName());
    }

    @Test
    void createsAStandaloneEventWithoutADiscipline() {
        CalendarEventResponse response = create(
                "Consulta médica",
                CalendarEventCategory.OTHER,
                MONDAY,
                null,
                null
        );

        assertNull(response.disciplineId());
        assertNull(response.disciplineName());
        assertNull(response.endsAt());
        assertEquals(dashboard.getId(), response.dashboardId());
    }

    @Test
    void trimsTheTitleAndTheDescription() {
        CalendarEventResponse response = calendarEventService.create(
                owner.getId(),
                dashboard.getId(),
                new CreateCalendarEventRequest(
                        "  Prova 1  ",
                        "  Capítulos 1 a 4  ",
                        CalendarEventCategory.EXAM,
                        MONDAY,
                        null,
                        null
                )
        );

        assertEquals("Prova 1", response.title());
        assertEquals("Capítulos 1 a 4", response.description());
    }

    @Test
    void acceptsAnEventWithoutADescription() {
        CalendarEventResponse response = create(
                "Entregar relatório",
                CalendarEventCategory.ASSIGNMENT,
                MONDAY.plusHours(15),
                null,
                null
        );

        assertNull(response.description());
    }

    @Test
    void refusesToLinkADisciplineFromAnotherDashboard() {
        Discipline outsider = newDiscipline(intruderDashboard, "Química");

        assertNotFound(() -> create(
                "Aula roubada",
                CalendarEventCategory.CLASS,
                MONDAY,
                null,
                outsider.getId()
        ));
    }

    @Test
    void failsWhenTheDisciplineDoesNotExist() {
        assertNotFound(() -> create(
                "Aula fantasma",
                CalendarEventCategory.CLASS,
                MONDAY,
                null,
                UUID.randomUUID()
        ));
    }

    @Test
    void hidesTheDashboardOfAnotherUser() {
        assertNotFound(() -> calendarEventService.create(
                intruder.getId(),
                dashboard.getId(),
                new CreateCalendarEventRequest(
                        "Evento intruso",
                        null,
                        CalendarEventCategory.OTHER,
                        MONDAY,
                        null,
                        null
                )
        ));
    }

    @Test
    void readsBackTheEventThatWasCreated() {
        CalendarEventResponse created = create(
                "Prova 1",
                CalendarEventCategory.EXAM,
                MONDAY,
                MONDAY.plusHours(2),
                discipline.getId()
        );

        CalendarEventResponse found = calendarEventService.findById(
                owner.getId(),
                dashboard.getId(),
                created.id()
        );

        assertEquals(created.id(), found.id());
        assertEquals("Prova 1", found.title());
        assertEquals(CalendarEventCategory.EXAM, found.category());
        assertEquals(MONDAY.plusHours(2), found.endsAt());
    }

    @Test
    void hidesTheEventOfAnotherUser() {
        CalendarEventResponse created = create(
                "Prova 1",
                CalendarEventCategory.EXAM,
                MONDAY,
                null,
                null
        );

        assertNotFound(() -> calendarEventService.findById(
                intruder.getId(),
                dashboard.getId(),
                created.id()
        ));
    }

    @Test
    void failsWhenTheEventDoesNotExist() {
        assertNotFound(() -> calendarEventService.findById(
                owner.getId(),
                dashboard.getId(),
                UUID.randomUUID()
        ));
    }

    @Test
    void unlinksTheDisciplineOnUpdate() {
        CalendarEventResponse created = create(
                "Aula de Cálculo",
                CalendarEventCategory.CLASS,
                MONDAY,
                MONDAY.plusHours(2),
                discipline.getId()
        );

        CalendarEventResponse updated = calendarEventService.update(
                owner.getId(),
                dashboard.getId(),
                created.id(),
                new UpdateCalendarEventRequest(
                        "Evento pessoal",
                        null,
                        CalendarEventCategory.OTHER,
                        MONDAY.plusDays(1),
                        null,
                        null
                )
        );

        assertEquals("Evento pessoal", updated.title());
        assertEquals(CalendarEventCategory.OTHER, updated.category());
        assertEquals(MONDAY.plusDays(1), updated.startsAt());
        assertNull(updated.disciplineId());
        assertNull(updated.endsAt());
    }

    @Test
    void refusesToUpdateTheEventOfAnotherUser() {
        CalendarEventResponse created = create(
                "Prova 1",
                CalendarEventCategory.EXAM,
                MONDAY,
                null,
                null
        );

        assertNotFound(() -> calendarEventService.update(
                intruder.getId(),
                dashboard.getId(),
                created.id(),
                new UpdateCalendarEventRequest(
                        "Sequestrado",
                        null,
                        CalendarEventCategory.OTHER,
                        MONDAY,
                        null,
                        null
                )
        ));
    }

    @Test
    void deletesTheEvent() {
        CalendarEventResponse created = create(
                "Prova 1",
                CalendarEventCategory.EXAM,
                MONDAY,
                null,
                null
        );

        calendarEventService.delete(owner.getId(), dashboard.getId(), created.id());

        assertNotFound(() -> calendarEventService.findById(
                owner.getId(),
                dashboard.getId(),
                created.id()
        ));
    }

    @Test
    void refusesToDeleteTheEventOfAnotherUser() {
        CalendarEventResponse created = create(
                "Prova 1",
                CalendarEventCategory.EXAM,
                MONDAY,
                null,
                null
        );

        assertNotFound(() -> calendarEventService.delete(
                intruder.getId(),
                dashboard.getId(),
                created.id()
        ));

        assertEquals(
                created.id(),
                calendarEventService.findById(owner.getId(), dashboard.getId(), created.id()).id()
        );
    }

    @Test
    void listsOnlyTheEventsInsideThePeriodOrderedByStart() {
        create("Depois", CalendarEventCategory.CLASS, MONDAY.plusDays(2), null, null);
        create("Antes", CalendarEventCategory.CLASS, MONDAY, null, null);
        create("Fora da semana", CalendarEventCategory.CLASS, MONDAY.plusDays(10), null, null);

        List<CalendarEventResponse> week = findByPeriod(MONDAY, MONDAY.plusDays(7), null);

        assertEquals(List.of("Antes", "Depois"), week.stream().map(CalendarEventResponse::title).toList());
    }

    // A aula que começa 23:00 de domingo e termina 01:00 de segunda pertence às duas visões:
    // ela ainda está acontecendo quando a semana começa.
    @Test
    void listsTheEventThatStartedBeforeThePeriodAndEndsInsideIt() {
        create("Aula da virada", CalendarEventCategory.CLASS, MONDAY.minusHours(2), MONDAY.plusHours(1), null);

        List<CalendarEventResponse> week = findByPeriod(MONDAY, MONDAY.plusDays(7), null);

        assertEquals(List.of("Aula da virada"), week.stream().map(CalendarEventResponse::title).toList());
    }

    @Test
    void ignoresTheEventThatEndedBeforeThePeriodStarted() {
        create("Aula da semana passada", CalendarEventCategory.CLASS, MONDAY.minusDays(3), MONDAY.minusDays(3).plusHours(2), null);

        assertTrue(findByPeriod(MONDAY, MONDAY.plusDays(7), null).isEmpty());
    }

    // Prazo não tem duração: vale como instante, então só entra no período em que cai.
    @Test
    void treatsADeadlineWithoutAnEndAsASingleMoment() {
        create("Entregar relatório", CalendarEventCategory.ASSIGNMENT, MONDAY.minusDays(1), null, null);

        assertTrue(findByPeriod(MONDAY, MONDAY.plusDays(7), null).isEmpty());
        assertEquals(1, findByPeriod(MONDAY.minusDays(2), MONDAY, null).size());
    }

    @Test
    void filtersTheEventsByCategory() {
        create("Aula", CalendarEventCategory.CLASS, MONDAY, null, null);
        create("Prova", CalendarEventCategory.EXAM, MONDAY.plusDays(1), null, null);
        create("Trabalho", CalendarEventCategory.ASSIGNMENT, MONDAY.plusDays(2), null, null);

        List<CalendarEventResponse> filtered = findByPeriod(
                MONDAY,
                MONDAY.plusDays(7),
                List.of(CalendarEventCategory.EXAM, CalendarEventCategory.ASSIGNMENT)
        );

        assertEquals(List.of("Prova", "Trabalho"), filtered.stream().map(CalendarEventResponse::title).toList());
    }

    @Test
    void treatsAnEmptyCategoryFilterAsNoFilter() {
        create("Aula", CalendarEventCategory.CLASS, MONDAY, null, null);
        create("Prova", CalendarEventCategory.EXAM, MONDAY.plusDays(1), null, null);

        assertEquals(2, findByPeriod(MONDAY, MONDAY.plusDays(7), List.of()).size());
    }

    @Test
    void rejectsAPeriodThatEndsBeforeItStarts() {
        assertBadRequest(() -> findByPeriod(MONDAY.plusDays(7), MONDAY, null));
        assertBadRequest(() -> findByPeriod(MONDAY, MONDAY, null));
    }

    @Test
    void doesNotLeakTheEventsOfAnotherDashboard() {
        calendarEventService.create(
                intruder.getId(),
                intruderDashboard.getId(),
                new CreateCalendarEventRequest(
                        "Prova do intruso",
                        null,
                        CalendarEventCategory.EXAM,
                        MONDAY,
                        null,
                        null
                )
        );

        assertTrue(findByPeriod(MONDAY, MONDAY.plusDays(7), null).isEmpty());
    }

    @Test
    void listsTheNextEventsIgnoringThePast() {
        create("Ontem", CalendarEventCategory.CLASS, LocalDateTime.now().minusDays(1), null, null);
        create("Depois de amanhã", CalendarEventCategory.EXAM, LocalDateTime.now().plusDays(2), null, null);
        create("Amanhã", CalendarEventCategory.CLASS, LocalDateTime.now().plusDays(1), null, null);

        List<CalendarEventResponse> upcoming = calendarEventService.findUpcoming(
                owner.getId(),
                dashboard.getId(),
                5
        );

        assertEquals(
                List.of("Amanhã", "Depois de amanhã"),
                upcoming.stream().map(CalendarEventResponse::title).toList()
        );
    }

    @Test
    void limitsTheNumberOfUpcomingEvents() {
        create("Primeiro", CalendarEventCategory.CLASS, LocalDateTime.now().plusDays(1), null, null);
        create("Segundo", CalendarEventCategory.CLASS, LocalDateTime.now().plusDays(2), null, null);
        create("Terceiro", CalendarEventCategory.CLASS, LocalDateTime.now().plusDays(3), null, null);

        List<CalendarEventResponse> upcoming = calendarEventService.findUpcoming(
                owner.getId(),
                dashboard.getId(),
                2
        );

        assertEquals(List.of("Primeiro", "Segundo"), upcoming.stream().map(CalendarEventResponse::title).toList());
    }

    @Test
    void rejectsAnUpcomingLimitOutOfRange() {
        assertBadRequest(() -> calendarEventService.findUpcoming(owner.getId(), dashboard.getId(), 0));
        assertBadRequest(() -> calendarEventService.findUpcoming(owner.getId(), dashboard.getId(), 51));
    }

    @Test
    void hidesTheCalendarOfAnotherUser() {
        assertNotFound(() -> findByPeriodAs(intruder.getId(), MONDAY, MONDAY.plusDays(7)));
        assertNotFound(() -> calendarEventService.findUpcoming(intruder.getId(), dashboard.getId(), 5));
    }

    private CalendarEventResponse create(
            String title,
            CalendarEventCategory category,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            UUID disciplineId
    ) {
        return calendarEventService.create(
                owner.getId(),
                dashboard.getId(),
                new CreateCalendarEventRequest(title, null, category, startsAt, endsAt, disciplineId)
        );
    }

    private List<CalendarEventResponse> findByPeriod(
            LocalDateTime start,
            LocalDateTime end,
            List<CalendarEventCategory> categories
    ) {
        return calendarEventService.findByPeriod(owner.getId(), dashboard.getId(), start, end, categories);
    }

    private List<CalendarEventResponse> findByPeriodAs(UUID userId, LocalDateTime start, LocalDateTime end) {
        return calendarEventService.findByPeriod(userId, dashboard.getId(), start, end, null);
    }

    private Discipline newDiscipline(Dashboard target, String name) {
        return disciplineRepository.save(new Discipline(
                name,
                "Professora Ana",
                60,
                new BigDecimal("6.00"),
                new BigDecimal("75.00"),
                target,
                List.of()
        ));
    }

    private void assertNotFound(Runnable access) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, access::run);

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    private void assertBadRequest(Runnable access) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, access::run);

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }
}
