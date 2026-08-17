package studdy.example.demo.discipline;

import java.math.BigDecimal;

public record AcademicPerformance(
        BigDecimal average,
        BigDecimal passingAverage,
        DisciplineStatus status
) {
}
