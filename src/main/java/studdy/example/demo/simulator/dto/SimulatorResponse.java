package studdy.example.demo.simulator.dto;

import java.math.BigDecimal;

public record SimulatorResponse(
        BigDecimal currentAverage,
        BigDecimal targetAverage,
        BigDecimal requiredGrade,
        boolean achievable
) {
}