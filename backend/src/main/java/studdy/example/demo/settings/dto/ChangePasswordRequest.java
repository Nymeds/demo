package studdy.example.demo.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import studdy.example.demo.security.MaxUtf8Bytes;

public record ChangePasswordRequest(
        @NotBlank(message = "Informe sua senha atual.")
        @Size(max = 72, message = "A senha deve ter no máximo 72 caracteres.")
        String currentPassword,

        // Mesmos limites do cadastro. @Size conta caracteres, mas o limite do BCrypt é em bytes:
        // letras acentuadas ocupam 2 bytes.
        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 8, max = 72, message = "A nova senha deve ter entre 8 e 72 caracteres.")
        @MaxUtf8Bytes(message = "A nova senha é longa demais. Letras acentuadas ocupam mais espaço; use menos caracteres.")
        String newPassword
) {
}
