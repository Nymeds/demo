package studdy.example.demo.simulator;

import org.junit.jupiter.api.Test;
import studdy.example.demo.discipline.AcademicPerformanceService;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineAccessService;
import studdy.example.demo.grade.Grade;
import studdy.example.demo.grade.GradeRepository;
import studdy.example.demo.simulator.dto.SimulationStatus;
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
        assertEquals(new BigDecimal("9.00"), response.requiredScoreRaw());
        assertEquals(SimulationStatus.ACHIEVABLE, response.status());
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
        assertEquals(new BigDecimal("15.00"), response.requiredScoreRaw());
        // (3 + 3 + 10) / 3 = 5.33
        assertEquals(new BigDecimal("5.33"), response.maxAchievableAverage());
        assertEquals(SimulationStatus.IMPOSSIBLE, response.status());
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
        assertTrue(response.achievable());
        assertEquals(new BigDecimal("-3.00"), response.requiredScoreRaw());
        assertEquals(SimulationStatus.ALREADY_REACHED, response.status());
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
        assertEquals(new BigDecimal("7.00"), response.requiredScoreRaw());
        assertEquals(SimulationStatus.ACHIEVABLE, response.status());
    }

    @Test
    void computesExactRequiredGradeFromRawScoresAvoidingRoundedSumPrecisionLoss() {
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

        /*
         * Notas cuja soma (20.00) dividida por 3 gera uma média
         * arredondada (6.67) que, multiplicada de volta por 3
         * (20.01), NÃO reproduz a soma original. Reconstruir a soma a
         * partir da média arredondada geraria um resultado diferente
         * do cálculo feito a partir das notas brutas.
         */
        Grade firstGrade = new Grade(
                null,
                "Prova 1",
                new BigDecimal("6.00"),
                LocalDate.of(2026, 8, 1)
        );
        Grade secondGrade = new Grade(
                null,
                "Prova 2",
                new BigDecimal("6.00"),
                LocalDate.of(2026, 8, 10)
        );
        Grade thirdGrade = new Grade(
                null,
                "Prova 3",
                new BigDecimal("8.00"),
                LocalDate.of(2026, 8, 20)
        );

        when(gradeRepository.findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(
                disciplineId
        )).thenReturn(List.of(firstGrade, secondGrade, thirdGrade));

        SimulatorRequest request =
                new SimulatorRequest(new BigDecimal("7.00"));

        SimulatorResponse response = service.simulate(
                userId,
                dashboardId,
                disciplineId,
                request
        );

        /*
         * Cálculo correto a partir das notas brutas:
         * soma = 20.00, alvo = 7.00 * 4 = 28.00
         * nota necessária = 28.00 - 20.00 = 8.00
         *
         * Se a soma fosse reconstruída a partir da média arredondada
         * (6.67 * 3 = 20.01), o resultado seria 28.00 - 20.01 = 7.99,
         * uma resposta incorreta por perda de precisão.
         */
        assertEquals(new BigDecimal("8.00"), response.requiredScoreRaw());
        assertEquals(new BigDecimal("8.00"), response.requiredGrade());
        assertEquals(SimulationStatus.ACHIEVABLE, response.status());
        assertTrue(response.achievable());
    }

    @Test
    void targetBelowPassingMinimumButWithinRangeIsAchievable() {
        UUID userId = UUID.randomUUID();
        UUID dashboardId = UUID.randomUUID();
        UUID disciplineId = UUID.randomUUID();

        Discipline discipline = mock(Discipline.class);

        // Média de aprovação alta; a meta simulada é menor que ela.
        when(discipline.getPassingAverage())
                .thenReturn(new BigDecimal("8.00"));

        when(disciplineAccessService.findOwnedDiscipline(
                userId,
                dashboardId,
                disciplineId
        )).thenReturn(discipline);

        Grade grade = new Grade(
                null,
                "Prova 1",
                new BigDecimal("4.00"),
                LocalDate.of(2026, 8, 1)
        );

        when(gradeRepository.findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(
                disciplineId
        )).thenReturn(List.of(grade));

        // Meta (5.00) abaixo da média de aprovação (8.00), mas atingível.
        SimulatorRequest request =
                new SimulatorRequest(new BigDecimal("5.00"));

        SimulatorResponse response = service.simulate(
                userId,
                dashboardId,
                disciplineId,
                request
        );

        // sum=4, target*2=10, required=6.00 -> dentro de [0,10].
        assertEquals(new BigDecimal("6.00"), response.requiredScoreRaw());
        assertEquals(SimulationStatus.ACHIEVABLE, response.status());
        assertTrue(response.achievable());
        // (4 + 10) / 2 = 7.00
        assertEquals(new BigDecimal("7.00"), response.maxAchievableAverage());
    }

    @Test
    void maxAchievableAverageUsesRawGradesRoundedHalfUp() {
        UUID userId = UUID.randomUUID();
        UUID dashboardId = UUID.randomUUID();
        UUID disciplineId = UUID.randomUUID();
        Discipline discipline = mock(Discipline.class);
        when(discipline.getPassingAverage()).thenReturn(new BigDecimal("6.00"));
        when(disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId))
                .thenReturn(discipline);
        when(gradeRepository.findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(disciplineId))
                .thenReturn(List.of(
                        new Grade(null, "P1", new BigDecimal("4.00"), LocalDate.of(2026, 8, 1)),
                        new Grade(null, "P2", new BigDecimal("5.00"), LocalDate.of(2026, 8, 10))
                ));

        SimulatorResponse response = service.simulate(
                userId, dashboardId, disciplineId, new SimulatorRequest(new BigDecimal("9.00")));

        // (4 + 5 + 10) / 3 = 6.333... -> 6.33
        assertEquals(new BigDecimal("6.33"), response.maxAchievableAverage());
        assertEquals(SimulationStatus.IMPOSSIBLE, response.status());
    }

    @Test
    void maxAchievableAverageIsTenWithoutGrades() {
        UUID userId = UUID.randomUUID();
        UUID dashboardId = UUID.randomUUID();
        UUID disciplineId = UUID.randomUUID();
        Discipline discipline = mock(Discipline.class);
        when(discipline.getPassingAverage()).thenReturn(new BigDecimal("6.00"));
        when(disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId))
                .thenReturn(discipline);
        when(gradeRepository.findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(disciplineId))
                .thenReturn(List.of());

        SimulatorResponse response = service.simulate(
                userId, dashboardId, disciplineId, new SimulatorRequest(new BigDecimal("8.00")));

        assertEquals(new BigDecimal("10.00"), response.maxAchievableAverage());
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