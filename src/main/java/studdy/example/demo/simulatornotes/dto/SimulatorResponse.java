package studdy.example.demo.simulatornotes.dto;

import java.math.BigDecimal;

public record SimulatorResponse(

        BigDecimal currentAverage,

        BigDecimal targetAverage,

        BigDecimal requiredGrade

) {
}