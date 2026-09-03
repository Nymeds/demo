package studdy.example.demo.simulator;

import org.junit.jupiter.api.Test;
import studdy.example.demo.discipline.AcademicPerformanceService;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineAccessService;
import studdy.example.demo.grade.Grade;
import studdy.example.demo.grade.GradeRepository;
import studdy.example.demo.simulator.dto.SimulatorRequest;
import studdy.example.demo.simulator.dto.SimulatorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertThrows;



import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimulatorServiceTest {

    private final GradeRepository gradeRepository = mock(GradeRepository.class);

    private final DisciplineAccessService disciplineAccessService =
            mock(DisciplineAccessService.class);

    private final AcademicPerformanceService academicPerformanceService =
            new AcademicPerformanceService();

    private final SimulatorService service = new SimulatorService(
            gradeRepository,
            disciplineAccessService,
            academicPerformanceService
    );

    @Test
    void calculatesRequiredGradeWhenTargetIsAchievable() {
        UUID userId = UUID.randomUUID();
        UUID dashboardId = UUID.randomUUID();
        UUID disciplineId = UUID.randomUUID();

        Grade firstGrade = new Grade(
                null,
                "Prova 1",
                new BigDecimal("7.00"),
                LocalDate.of(2026, 8, 1)
        );

        Grade secondGrade = new Grade(
                null,
                "Prova 2",
                new BigDecimal("8.00"),
                LocalDate.of(2026, 8, 10)
        );

        Discipline discipline = mock(Discipline.class);

        when(discipline.getPassingAverage())
                .thenReturn(new BigDecimal("6.00"));

        when(disciplineAccessService.findOwnedDiscipline(
                userId,
                dashboardId,
                disciplineId
        )).thenReturn(discipline);

        when(gradeRepository.findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(
                disciplineId
        )).thenReturn(List.of(firstGrade, secondGrade));

        SimulatorRequest request =
                new SimulatorRequest(new BigDecimal("8.00"));

        SimulatorResponse response = service.simulate(
                userId,
                dashboardId,
                disciplineId,
                request
        );

        assertEquals(new BigDecimal("7.50"), response.currentAverage());
        assertEquals(new BigDecimal("8.00"), response.targetAverage());
        assertEquals(new BigDecimal("9.00"), response.requiredGrade());
        assertTrue(response.achievable());
    }

    @Test
    void marksTargetAsUnachievableWhenRequiredGradeIsAboveTen() {
        UUID userId = UUID.randomUUID();
        UUID dashboardId = UUID.randomUUID();
        UUID disciplineId = UUID.randomUUID();

        Discipline discipline = mock(Discipline.class);

        when(discipline.getPassingAverage())
                .thenReturn(new BigDecimal("6.00"));

        when(disciplineAccessService.findOwnedDiscipline(
                userId,
                dashboardId,
                disciplineId
        )).thenReturn(discipline);

        Grade firstGrade = new Grade(
                null,
                "Prova 1",
                new BigDecimal("3.00"),
                LocalDate.of(2026, 8, 1)
        );

        Grade secondGrade = new Grade(
                null,
                "Prova 2",
                new BigDecimal("3.00"),
                LocalDate.of(2026, 8, 10)
        );

        when(gradeRepository.findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(
                disciplineId
        )).thenReturn(List.of(firstGrade, secondGrade));

        SimulatorRequest request =
                new SimulatorRequest(new BigDecimal("7.00"));

        SimulatorResponse response = service.simulate(
                userId,
                dashboardId,
                disciplineId,
                request
        );

        assertEquals(new BigDecimal("10.00"), response.requiredGrade());
        assertFalse(response.achievable());
    }

    @Test
    void marksTargetAsUnachievableWhenRequiredGradeIsNegative() {
        UUID userId = UUID.randomUUID();
        UUID dashboardId = UUID.randomUUID();
        UUID disciplineId = UUID.randomUUID();

        Discipline discipline = mock(Discipline.class);

        when(discipline.getPassingAverage())
                .thenReturn(new BigDecimal("6.00"));

        when(disciplineAccessService.findOwnedDiscipline(
                userId,
                dashboardId,
                disciplineId
        )).thenReturn(discipline);

        Grade firstGrade = new Grade(
                null,
                "Prova 1",
                new BigDecimal("9.00"),
                LocalDate.of(2026, 8, 1)
        );

        Grade secondGrade = new Grade(
                null,
                "Prova 2",
                new BigDecimal("9.00"),
                LocalDate.of(2026, 8, 10)
        );

        when(gradeRepository.findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(
                disciplineId
        )).thenReturn(List.of(firstGrade, secondGrade));

        SimulatorRequest request =
                new SimulatorRequest(new BigDecimal("5.00"));

        SimulatorResponse response = service.simulate(
                userId,
                dashboardId,
                disciplineId,
                request
        );

        assertEquals(new BigDecimal("0.00"), response.requiredGrade());
        assertFalse(response.achievable());
    }

    @Test
    void targetEqualToPassingAverageIsValid() {
        UUID userId = UUID.randomUUID();
        UUID dashboardId = UUID.randomUUID();
        UUID disciplineId = UUID.randomUUID();

        Discipline discipline = mock(Discipline.class);

        when(discipline.getPassingAverage())
                .thenReturn(new BigDecimal("6.00"));

        when(disciplineAccessService.findOwnedDiscipline(
                userId,
                dashboardId,
                disciplineId
        )).thenReturn(discipline);

        Grade grade = new Grade(
                null,
                "Prova 1",
                new BigDecimal("5.00"),
                LocalDate.of(2026, 8, 1)
        );

        when(gradeRepository.findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(
                disciplineId
        )).thenReturn(List.of(grade));

        SimulatorRequest request =
                new SimulatorRequest(new BigDecimal("6.00"));

        SimulatorResponse response = service.simulate(
                userId,
                dashboardId,
                disciplineId,
                request
        );

        assertEquals(new BigDecimal("7.00"), response.requiredGrade());
        assertTrue(response.achievable());
    }
@Test
void returnsNotFoundWhenDisciplineBelongsToAnotherUser() {
    UUID userId = UUID.randomUUID();
    UUID dashboardId = UUID.randomUUID();
    UUID disciplineId = UUID.randomUUID();

    when(disciplineAccessService.findOwnedDiscipline(
            userId,
            dashboardId,
            disciplineId
    )).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

    SimulatorRequest request =
            new SimulatorRequest(new BigDecimal("6.00"));

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.simulate(
                    userId,
                    dashboardId,
                    disciplineId,
                    request
            )
    );

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
}

}