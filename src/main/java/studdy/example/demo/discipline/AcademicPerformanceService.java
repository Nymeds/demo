package studdy.example.demo.discipline;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import studdy.example.demo.grade.Grade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

@Service
public class AcademicPerformanceService {

    private final BigDecimal passingAverage;

    public AcademicPerformanceService(
            @Value("${academic.passing-average:6.00}") BigDecimal passingAverage
    ) {
        if (passingAverage.compareTo(BigDecimal.ZERO) < 0
                || passingAverage.compareTo(new BigDecimal("10.00")) > 0) {
            throw new IllegalArgumentException("A média de aprovação deve estar entre 0 e 10.");
        }

        this.passingAverage = passingAverage.setScale(2, RoundingMode.HALF_UP);
    }

    public AcademicPerformance calculate(Collection<Grade> grades) {
        if (grades.isEmpty()) {
            return new AcademicPerformance(null, passingAverage, DisciplineStatus.NO_DATA);
        }

        BigDecimal totalScores = grades.stream()
                .map(Grade::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = totalScores.divide(
                BigDecimal.valueOf(grades.size()),
                2,
                RoundingMode.HALF_UP
        );

        DisciplineStatus status = average.compareTo(passingAverage) >= 0
                ? DisciplineStatus.APPROVED
                : DisciplineStatus.FAILED_BY_GRADE;

        return new AcademicPerformance(average, passingAverage, status);
    }
}
