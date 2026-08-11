package studdy.example.demo.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import studdy.example.demo.dashboard.DashboardStatus;

public record CreateDashboardRequest(
        @NotBlank(message = "O nome do dashboard é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String name,

        @NotNull(message = "O status do dashboard é obrigatório.")
        DashboardStatus status
) {
}
