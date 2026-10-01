package studdy.example.demo.discipline.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateFrequencyRequest(

    @NotNull(message = "A quantidade de faltas é obrigatória.")
    @PositiveOrZero(message = "A quantidade de faltas não pode ser negativa.")
    @Max(value = 999, message = "A quantidade de faltas deve ser no máximo 999.")
    Integer absences

) {
}
