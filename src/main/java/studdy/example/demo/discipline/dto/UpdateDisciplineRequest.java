package studdy.example.demo.discipline.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateDisciplineRequest(
        @NotBlank(message = "O nome da disciplina é obrigatório.")
        @Size(max = 120, message = "O nome da disciplina deve ter no máximo 120 caracteres.")
        String name,

        @NotBlank(message = "O nome do professor é obrigatório.")
        @Size(max = 120, message = "O nome do professor deve ter no máximo 120 caracteres.")
        String professorName,

        @NotNull(message = "A carga horária é obrigatória.")
        @Positive(message = "A carga horária deve ser maior que zero.")
        Integer workloadHours,

        @NotNull(message = "A lista de horários é obrigatória.")
        @Size(min = 1, message = "A disciplina deve ter pelo menos um horário.")
        List<@Valid ClassScheduleRequest> schedules
) {
}
