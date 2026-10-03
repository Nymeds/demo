package studdy.example.demo.discipline;

import org.springframework.stereotype.Service;
import studdy.example.demo.grade.Grade;
import studdy.example.demo.grade.GradeRounding;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

@Service
public class AcademicPerformanceService {

    public AcademicPerformance calculate(Collection<Grade> grades, BigDecimal passingAverage) {
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

        // Mesma média arredondada que o estudante vê (e que define a faixa de desempenho).
        DisciplineStatus status = GradeRounding.displayAverage(average).compareTo(passingAverage) >= 0
                ? DisciplineStatus.APPROVED
                : DisciplineStatus.FAILED_BY_GRADE;

        return new AcademicPerformance(average, passingAverage, status);
    }
}
