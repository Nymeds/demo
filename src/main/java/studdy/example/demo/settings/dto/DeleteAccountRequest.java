package studdy.example.demo.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(
        @NotBlank(message = "Informe sua senha atual para excluir a conta.")
        @Size(max = 72, message = "A senha deve ter no máximo 72 caracteres.")
        String currentPassword
) {
}
