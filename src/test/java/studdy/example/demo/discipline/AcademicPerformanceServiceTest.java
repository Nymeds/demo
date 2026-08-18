package studdy.example.demo.discipline;

import org.junit.jupiter.api.Test;
import studdy.example.demo.grade.Grade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AcademicPerformanceServiceTest {

    private static final BigDecimal PASSING_AVERAGE = new BigDecimal("6.00");

    private final AcademicPerformanceService service = new AcademicPerformanceService();

    @Test
    void returnsNoDataWhenThereAreNoRecordedGrades() {
        AcademicPerformance performance = service.calculate(List.of(), PASSING_AVERAGE);

        assertNull(performance.average());
        assertEquals(new BigDecimal("6.00"), performance.passingAverage());
        assertEquals(DisciplineStatus.NO_DATA, performance.status());
    }

    @Test
    void calculatesSimpleAverageAndApprovesWhenMinimumIsReached() {
        Grade firstGrade = new Grade(null, "Prova 1", new BigDecimal("7.00"), LocalDate.of(2026, 8, 1));
        Grade secondGrade = new Grade(null, "Trabalho final", new BigDecimal("9.00"), LocalDate.of(2026, 8, 10));

        AcademicPerformance performance = service.calculate(List.of(firstGrade, secondGrade), PASSING_AVERAGE);

        assertEquals(new BigDecimal("8.00"), performance.average());
        assertEquals(DisciplineStatus.APPROVED, performance.status());
    }

    @Test
    void returnsFailedByGradeWhenAverageIsBelowMinimum() {
        Grade grade = new Grade(null, "Prova 1", new BigDecimal("5.50"), LocalDate.of(2026, 8, 1));

        AcademicPerformance performance = service.calculate(List.of(grade), PASSING_AVERAGE);

        assertEquals(new BigDecimal("5.50"), performance.average());
        assertEquals(DisciplineStatus.FAILED_BY_GRADE, performance.status());
    }

    @Test
    void appliesThePassingAverageDefinedByEachDiscipline() {
        Grade grade = new Grade(null, "Prova 1", new BigDecimal("7.00"), LocalDate.of(2026, 8, 1));

        AcademicPerformance lenient = service.calculate(List.of(grade), new BigDecimal("6.00"));
        AcademicPerformance strict = service.calculate(List.of(grade), new BigDecimal("8.00"));

        assertEquals(DisciplineStatus.APPROVED, lenient.status());
        assertEquals(DisciplineStatus.FAILED_BY_GRADE, strict.status());
    }
}
