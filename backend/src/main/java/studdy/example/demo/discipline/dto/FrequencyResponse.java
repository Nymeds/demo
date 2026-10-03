package studdy.example.demo.discipline.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FrequencyResponse(
    UUID id,
    UUID disciplineId,
    Integer absences,
    BigDecimal attendancePercentage,
    BigDecimal lossPerAbsence,
    Integer maximumAbsences
) {
}
