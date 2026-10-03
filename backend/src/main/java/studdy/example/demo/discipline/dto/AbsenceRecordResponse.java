package studdy.example.demo.discipline.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import studdy.example.demo.discipline.AbsenceRecord;

public record AbsenceRecordResponse(
        UUID id,
        UUID disciplineId,
        String disciplineName,
        LocalDate date,
        Integer quantity,
        String reason,
        String note,
        BigDecimal impact,
        Instant createdAt
) {
    public static AbsenceRecordResponse from(AbsenceRecord record) {
        return new AbsenceRecordResponse(
                record.getId(),
                record.getDiscipline().getId(),
                record.getDiscipline().getName(),
                record.getAbsenceDate(),
                record.getQuantity(),
                record.getReason(),
                record.getNote(),
                record.getImpactPercentage(),
                record.getCreatedAt()
        );
    }
}
