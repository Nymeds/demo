package studdy.example.demo.simulatornotes.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SimulatorResponse(

        UUID disciplineId,

        BigDecimal currentAverage,

        BigDecimal targetAverage,

        BigDecimal requiredGrade

) {
}