package studdy.example.demo.discipline;

import org.junit.jupiter.api.Test;
import studdy.example.demo.grade.Grade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AcademicPerformanceServiceTest {

    private final AcademicPerformanceService service = new AcademicPerformanceService(new BigDecimal("6.00"));

    @Test
    void returnsNoDataWhenThereAreNoRecordedGrades() {
        AcademicPerformance performance = service.calculate(List.of());

        assertNull(performance.average());
        assertEquals(new BigDecimal("6.00"), performance.passingAverage());
        assertEquals(DisciplineStatus.NO_DATA, performance.status());
    }

    @Test
    void calculatesSimpleAverageAndApprovesWhenMinimumIsReached() {
        Grade firstGrade = new Grade(null, "Prova 1", new BigDecimal("7.00"), LocalDate.of(2026, 8, 1));
        Grade secondGrade = new Grade(null, "Trabalho final", new BigDecimal("9.00"), LocalDate.of(2026, 8, 10));

        AcademicPerformance performance = service.calculate(List.of(firstGrade, secondGrade));

        assertEquals(new BigDecimal("8.00"), performance.average());
        assertEquals(DisciplineStatus.APPROVED, performance.status());
    }

    @Test
    void returnsFailedByGradeWhenAverageIsBelowMinimum() {
        Grade grade = new Grade(null, "Prova 1", new BigDecimal("5.50"), LocalDate.of(2026, 8, 1));

        AcademicPerformance performance = service.calculate(List.of(grade));

        assertEquals(new BigDecimal("5.50"), performance.average());
        assertEquals(DisciplineStatus.FAILED_BY_GRADE, performance.status());
    }
}
