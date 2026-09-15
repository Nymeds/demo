package studdy.example.demo.gradebook;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// Uma linha da consulta agregada: a disciplina com a soma, a quantidade e a data da última nota.
public record GradebookRow(
        UUID disciplineId,
        String name,
        String professorName,
        String color,
        String semester,
        String periodo,
        BigDecimal passingAverage,
        BigDecimal scoreSum,
        Long gradeCount,
        LocalDate lastRecordedAt
) {
}
