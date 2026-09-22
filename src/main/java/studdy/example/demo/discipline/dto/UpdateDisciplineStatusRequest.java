package studdy.example.demo.discipline.dto;

import jakarta.validation.constraints.NotNull;
import studdy.example.demo.discipline.DisciplineLifecycleStatus;

public record UpdateDisciplineStatusRequest(
        @NotNull(message = "A situação da disciplina é obrigatória.")
        DisciplineLifecycleStatus status
) {
}
