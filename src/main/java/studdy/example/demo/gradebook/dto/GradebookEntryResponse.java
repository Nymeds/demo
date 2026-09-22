package studdy.example.demo.gradebook.dto;

import studdy.example.demo.gradebook.GradebookRow;
import studdy.example.demo.gradebook.PerformanceBand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

public record GradebookEntryResponse(
        UUID disciplineId,
        String name,
        String professorName,
        String color,
        String semester,
        String periodo,
        BigDecimal passingAverage,
        BigDecimal average,
        long gradeCount,
        LocalDate lastRecordedAt,
        PerformanceBand band,
        boolean passing
) {

    public static GradebookEntryResponse from(GradebookRow row) {
        long gradeCount = row.gradeCount() == null ? 0 : row.gradeCount();
        // Mesmo cálculo do AcademicPerformanceService: média simples com duas casas, HALF_UP.
        BigDecimal average = gradeCount == 0
                ? null
                : row.scoreSum().divide(BigDecimal.valueOf(gradeCount), 2, RoundingMode.HALF_UP);

        return new GradebookEntryResponse(
                row.disciplineId(),
                row.name(),
                row.professorName(),
                row.color(),
                row.semester(),
                row.periodo(),
                row.passingAverage(),
                average,
                gradeCount,
                row.lastRecordedAt(),
                PerformanceBand.of(average),
                average != null && average.compareTo(row.passingAverage()) >= 0
        );
    }
}
