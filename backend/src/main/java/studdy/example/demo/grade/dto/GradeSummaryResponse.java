package studdy.example.demo.grade.dto;

import studdy.example.demo.discipline.AcademicPerformance;

import java.math.BigDecimal;
import java.util.UUID;

public record GradeSummaryResponse(
        UUID disciplineId,
        Integer recordedGrades,
        BigDecimal average,
        BigDecimal passingAverage,
        studdy.example.demo.discipline.DisciplineStatus academicSituation
) {
    public static GradeSummaryResponse from(
            UUID disciplineId,
            Integer recordedGrades,
            AcademicPerformance performance
    ) {
        return new GradeSummaryResponse(
                disciplineId,
                recordedGrades,
                performance.average(),
                performance.passingAverage(),
                performance.status()
        );
    }
}
