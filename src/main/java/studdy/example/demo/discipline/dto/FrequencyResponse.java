package studdy.example.demo.discipline.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FrequencyResponse(
    UUID id,
    UUID disciplineId,
    Integer totalClasses,
    Integer absences,
    BigDecimal attendancePercentage,
    Integer minimumAttendanceClasses,
    Integer maximumAbsences
) {
}
