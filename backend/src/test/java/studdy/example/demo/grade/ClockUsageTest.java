package studdy.example.demo.grade;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.activities.Activity;
import studdy.example.demo.activities.ActivityRepository;
import studdy.example.demo.calendar.CalendarEventRepository;
import studdy.example.demo.calendar.CalendarEventService;
import studdy.example.demo.discipline.AcademicPerformanceService;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineAccessService;
import studdy.example.demo.grade.dto.CreateGradeRequest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// "Hoje" vem do Clock de America/Sao_Paulo, não do fuso da JVM: perto da meia-noite UTC os dois divergem.
class ClockUsageTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    // 2026-09-29T02:30Z == 28/09 23:30 em São Paulo (ainda é dia 28 lá, já é dia 29 em UTC).
    private static final Clock BEFORE_MIDNIGHT_SP = Clock.fixed(Instant.parse("2026-09-29T02:30:00Z"), SAO_PAULO);
    // 2026-09-29T03:30Z == 29/09 00:30 em São Paulo.
    private static final Clock AFTER_MIDNIGHT_SP = Clock.fixed(Instant.parse("2026-09-29T03:30:00Z"), SAO_PAULO);

    private final UUID userId = UUID.randomUUID();
    private final UUID dashboardId = UUID.randomUUID();
    private final UUID disciplineId = UUID.randomUUID();
    private final UUID activityId = UUID.randomUUID();
    private final GradeRepository gradeRepository = mock(GradeRepository.class);
    private final ActivityRepository activityRepository = mock(ActivityRepository.class);
    private final DisciplineAccessService access = mock(DisciplineAccessService.class);

    private GradeService serviceWith(Clock clock) {
        Discipline discipline = mock(Discipline.class);
        when(discipline.getId()).thenReturn(disciplineId);
        when(access.findOwnedDiscipline(userId, dashboardId, disciplineId)).thenReturn(discipline);
        Activity activity = new Activity("Prova", null, LocalDate.of(2026, 9, 29), null, null, discipline);
        when(activityRepository.findByIdAndDiscipline_Id(activityId, disciplineId)).thenReturn(Optional.of(activity));
        when(gradeRepository.saveAndFlush(any(Grade.class))).thenAnswer(i -> i.getArgument(0));
        return new GradeService(gradeRepository, activityRepository, access, new AcademicPerformanceService(), clock);
    }

    private CreateGradeRequest request() {
        return new CreateGradeRequest("Prova", new BigDecimal("8.00"), LocalDate.of(2026, 9, 29), activityId);
    }

    @Test
    void gradeIsRejectedWhileTheActivityDayHasNotStartedInSaoPaulo() {
        GradeService service = serviceWith(BEFORE_MIDNIGHT_SP);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(userId, dashboardId, disciplineId, request()));

        assertEquals(400, error.getStatusCode().value());
    }

    @Test
    void gradeIsAcceptedOnceMidnightPassesInSaoPaulo() {
        GradeService service = serviceWith(AFTER_MIDNIGHT_SP);

        assertEquals("Prova", service.create(userId, dashboardId, disciplineId, request()).assessmentName());
    }

    @Test
    void upcomingEventsAreFilteredFromTheSaoPauloWallClock() {
        CalendarEventRepository repository = mock(CalendarEventRepository.class);
        DisciplineAccessService calendarAccess = mock(DisciplineAccessService.class);
        when(repository.findAllByDashboard_IdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(any(), any(), any()))
                .thenReturn(List.of());
        CalendarEventService service = new CalendarEventService(repository, calendarAccess, BEFORE_MIDNIGHT_SP);

        service.findUpcoming(userId, dashboardId, 5, null);

        verify(repository).findAllByDashboard_IdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(
                eq(dashboardId), eq(LocalDateTime.of(2026, 9, 28, 23, 30)), any());
    }
}
