package studdy.example.demo.discipline.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateFrequencyRequest(

    @NotNull(message = "A quantidade total de aulas é obrigatória.")
    @Positive(message = "A quantidade total de aulas deve ser maior que zero.")
    Integer totalClasses,

    @NotNull(message = "A quantidade de faltas é obrigatória.")
    @PositiveOrZero(message = "A quantidade de faltas não pode ser negativa.")
    Integer absences

) {
}
