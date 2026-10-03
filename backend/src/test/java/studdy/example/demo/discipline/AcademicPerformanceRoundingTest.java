package studdy.example.demo.discipline;

import org.junit.jupiter.api.Test;
import studdy.example.demo.grade.Grade;
import studdy.example.demo.gradebook.PerformanceBand;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// A decisão de aprovação usa o mesmo arredondamento (1 casa, HALF_UP) que a faixa de desempenho.
class AcademicPerformanceRoundingTest {

    private final AcademicPerformanceService service = new AcademicPerformanceService();

    private static Grade grade(String score) {
        return new Grade(null, "Prova", new BigDecimal(score), LocalDate.of(2026, 8, 1));
    }

    private DisciplineStatus statusFor(String first, String second, String passing) {
        return service.calculate(List.of(grade(first), grade(second)), new BigDecimal(passing)).status();
    }

    @Test
    void approvesWhenAverageRoundsUpToThePassingAverage() {
        // 6.95 exibido como 7.0
        assertEquals(DisciplineStatus.APPROVED, statusFor("6.90", "7.00", "7.00"));
        assertEquals(PerformanceBand.GOOD, PerformanceBand.of(new BigDecimal("6.95")));
        // 8.95 exibido como 9.0
        assertEquals(DisciplineStatus.APPROVED, statusFor("8.90", "9.00", "9.00"));
        assertEquals(PerformanceBand.EXCELLENT, PerformanceBand.of(new BigDecimal("8.95")));
        // 4.95 exibido como 5.0
        assertEquals(DisciplineStatus.APPROVED, statusFor("4.90", "5.00", "5.00"));
        assertEquals(PerformanceBand.REGULAR, PerformanceBand.of(new BigDecimal("4.95")));
    }

    @Test
    void failsWhenAverageRoundsDownBelowThePassingAverage() {
        // 6.94 exibido como 6.9
        assertEquals(DisciplineStatus.FAILED_BY_GRADE, statusFor("6.90", "6.98", "7.00"));
        assertEquals(DisciplineStatus.FAILED_BY_GRADE, statusFor("8.90", "8.98", "9.00"));
        assertEquals(DisciplineStatus.FAILED_BY_GRADE, statusFor("4.90", "4.98", "5.00"));
    }
}
