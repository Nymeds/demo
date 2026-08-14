package studdy.example.demo.grade;

import org.junit.jupiter.api.Test;
import studdy.example.demo.discipline.DisciplineStatus;
import studdy.example.demo.grade.dto.GradeSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GradeSummaryResponseTest {

    @Test
    void returnsNoDataWhenThereAreNoRecordedGrades() {
        UUID disciplineId = UUID.randomUUID();

        GradeSummaryResponse summary = GradeSummaryResponse.from(disciplineId, List.of());

        assertEquals(disciplineId, summary.disciplineId());
        assertEquals(0, summary.recordedGrades());
        assertNull(summary.weightedAverage());
        assertEquals(new BigDecimal("6.00"), summary.passingAverage());
        assertEquals(DisciplineStatus.NO_DATA, summary.academicSituation());
    }

    @Test
    void calculatesWeightedAverageAndApprovesWhenMinimumIsReached() {
        Grade firstGrade = new Grade(
                null,
                "Prova 1",
                new BigDecimal("7.00"),
                new BigDecimal("1.00"),
                LocalDate.of(2026, 8, 1)
        );
        Grade secondGrade = new Grade(
                null,
                "Trabalho final",
                new BigDecimal("9.00"),
                new BigDecimal("3.00"),
                LocalDate.of(2026, 8, 10)
        );

        GradeSummaryResponse summary = GradeSummaryResponse.from(
                UUID.randomUUID(),
                List.of(firstGrade, secondGrade)
        );

        assertEquals(new BigDecimal("8.50"), summary.weightedAverage());
        assertEquals(DisciplineStatus.APPROVED, summary.academicSituation());
    }

    @Test
    void keepsSituationInProgressWhenAverageIsBelowMinimum() {
        Grade grade = new Grade(
                null,
                "Prova 1",
                new BigDecimal("5.50"),
                new BigDecimal("1.00"),
                LocalDate.of(2026, 8, 1)
        );

        GradeSummaryResponse summary = GradeSummaryResponse.from(
                UUID.randomUUID(),
                List.of(grade)
        );

        assertEquals(new BigDecimal("5.50"), summary.weightedAverage());
        assertEquals(DisciplineStatus.IN_PROGRESS, summary.academicSituation());
    }
}
