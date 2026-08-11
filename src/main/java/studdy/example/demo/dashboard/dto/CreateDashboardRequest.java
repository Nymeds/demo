package studdy.example.demo.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import studdy.example.demo.dashboard.DashboardStatus;

import java.util.List;

public record CreateDashboardRequest(
        @NotBlank(message = "O nome do dashboard é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String name,

        @NotNull(message = "O status do dashboard é obrigatório.")
        DashboardStatus status,

        @NotNull(message = "A lista de disciplinas é obrigatória.")
        @Size(max = 30, message = "Um dashboard pode ter no máximo 30 disciplinas.")
        List<@NotBlank(message = "O nome da disciplina não pode ser vazio.")
                @Size(max = 120, message = "O nome da disciplina deve ter no máximo 120 caracteres.") String> disciplines
) {
}
