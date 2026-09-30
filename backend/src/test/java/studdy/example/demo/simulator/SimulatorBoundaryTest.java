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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Status é decidido sobre a nota necessária exata; apenas as saídas são arredondadas.
class SimulatorBoundaryTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID dashboardId = UUID.randomUUID();
    private final UUID disciplineId = UUID.randomUUID();
    private final GradeRepository gradeRepository = mock(GradeRepository.class);
    private final DisciplineAccessService access = mock(DisciplineAccessService.class);
    private final SimulatorService service =
            new SimulatorService(gradeRepository, access, new AcademicPerformanceService());

    private SimulatorResponse simulate(String target, String... scores) {
        Discipline discipline = mock(Discipline.class);
        when(discipline.getPassingAverage()).thenReturn(new BigDecimal("6.00"));
        when(access.findOwnedDiscipline(userId, dashboardId, disciplineId)).thenReturn(discipline);
        List<Grade> grades = Arrays.stream(scores)
                .map(s -> new Grade(null, "P", new BigDecimal(s), LocalDate.of(2026, 8, 1)))
                .toList();
        when(gradeRepository.findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(disciplineId))
                .thenReturn(grades);
        return service.simulate(userId, dashboardId, disciplineId, new SimulatorRequest(new BigDecimal(target)));
    }

    @Test
    void requiredScoreOfExactlyZeroIsAlreadyReached() {
        SimulatorResponse r = simulate("5.00", "10.00");
        assertEquals(SimulationStatus.ALREADY_REACHED, r.status());
        assertEquals(new BigDecimal("0.00"), r.requiredScoreRaw());
        assertTrue(r.achievable());
    }

    @Test
    void requiredScoreOfExactlyTenIsAchievable() {
        SimulatorResponse r = simulate("5.00", "0.00");
        assertEquals(SimulationStatus.ACHIEVABLE, r.status());
        assertEquals(new BigDecimal("10.00"), r.requiredScoreRaw());
        assertEquals(new BigDecimal("10.00"), r.requiredGrade());
        assertTrue(r.achievable());
    }

    @Test
    void requiredScoreJustAboveTenIsImpossibleEvenThoughItDisplaysAsTen() {
        // 5.002 * 2 - 0 = 10.004
        SimulatorResponse r = simulate("5.002", "0.00");
        assertEquals(SimulationStatus.IMPOSSIBLE, r.status());
        assertFalse(r.achievable());
        assertEquals(new BigDecimal("10.00"), r.requiredScoreRaw());
        assertEquals(new BigDecimal("10.00"), r.requiredGrade());
    }

    @Test
    void requiredScoreJustBelowZeroIsAlreadyReached() {
        // 4.998 * 2 - 10 = -0.004
        SimulatorResponse r = simulate("4.998", "10.00");
        assertEquals(SimulationStatus.ALREADY_REACHED, r.status());
        assertEquals(new BigDecimal("0.00"), r.requiredGrade());
    }

    @Test
    void withoutGradesTheFirstGradeMustEqualTheTarget() {
        SimulatorResponse r = simulate("7.00");
        assertEquals(SimulationStatus.ACHIEVABLE, r.status());
        assertEquals(new BigDecimal("7.00"), r.requiredScoreRaw());
    }
}
