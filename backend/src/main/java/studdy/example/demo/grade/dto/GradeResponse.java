package studdy.example.demo.grade.dto;

import studdy.example.demo.grade.Grade;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GradeResponse(
        UUID id,
        UUID disciplineId,
        String assessmentName,
        BigDecimal score,
        LocalDate recordedAt,
        Instant createdAt,
        Instant updatedAt,
        UUID activityId,
        String observation
) {

    public static GradeResponse from(Grade grade) {
        return new GradeResponse(
                grade.getId(),
                grade.getDiscipline().getId(),
                grade.getAssessmentName(),
                grade.getScore(),
                grade.getRecordedAt(),
                grade.getCreatedAt(),
                grade.getUpdatedAt(),
                // getId() de um proxy LAZY não dispara consulta à tabela de atividades.
                grade.getActivity() == null ? null : grade.getActivity().getId(),
                grade.getObservation()
        );
    }
}
