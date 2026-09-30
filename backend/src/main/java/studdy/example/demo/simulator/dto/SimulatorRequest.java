package studdy.example.demo.simulator.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SimulatorRequest(

        @NotNull(message = "A média desejada é obrigatória.")
        @DecimalMin(value = "0.00", message = "A média não pode ser negativa.")
        @DecimalMax(value = "10.00", message = "A média não pode ser maior que 10.")
        BigDecimal targetAverage

) {
}