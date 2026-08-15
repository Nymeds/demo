package studdy.example.demo.discipline.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateDisciplineRequest(
        @NotBlank(message = "O nome da disciplina é obrigatório.")
        @Size(max = 120, message = "O nome da disciplina deve ter no máximo 120 caracteres.")
        String name,

        @Size(max = 120, message = "O nome do professor deve ter no máximo 120 caracteres.")
        String professorName,

        @NotBlank(message = "A cor da disciplina é obrigatória.")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "A cor da disciplina deve estar no formato hexadecimal.")
        String color,

        @NotNull(message = "A lista de horários é obrigatória.")
        @Size(min = 1, message = "A disciplina deve ter pelo menos um horário.")
        List<@Valid ClassScheduleRequest> schedules
) {
}
