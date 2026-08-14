package studdy.example.demo.grade.dto;

import studdy.example.demo.discipline.DisciplineStatus;
import studdy.example.demo.grade.Grade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

public record GradeSummaryResponse(
        UUID disciplineId,
        Integer recordedGrades,
        BigDecimal weightedAverage,
        BigDecimal passingAverage,
        DisciplineStatus academicSituation
) {

    private static final BigDecimal PASSING_AVERAGE = new BigDecimal("6.00");

    public static GradeSummaryResponse from(UUID disciplineId, List<Grade> grades) {
        if (grades.isEmpty()) {
            return new GradeSummaryResponse(
                    disciplineId,
                    0,
                    null,
                    PASSING_AVERAGE,
                    DisciplineStatus.NO_DATA
            );
        }

        BigDecimal totalWeight = grades.stream()
                .map(Grade::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal weightedScore = grades.stream()
                .map(grade -> grade.getScore().multiply(grade.getWeight()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = weightedScore.divide(totalWeight, 2, RoundingMode.HALF_UP);
        DisciplineStatus situation = average.compareTo(PASSING_AVERAGE) >= 0
                ? DisciplineStatus.APPROVED
                : DisciplineStatus.IN_PROGRESS;

        return new GradeSummaryResponse(
                disciplineId,
                grades.size(),
                average,
                PASSING_AVERAGE,
                situation
        );
    }
}
