package studdy.example.demo.settings.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

public record ChangePasswordRequest(
        @NotBlank(message = "Informe sua senha atual.")
        @Size(max = 72, message = "A senha deve ter no máximo 72 caracteres.")
        String currentPassword,

        // Mesmos limites do cadastro. O teto de 72 vem do BCrypt, que ignora o que passa disso.
        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 8, max = 72, message = "A nova senha deve ter entre 8 e 72 caracteres.")
        String newPassword
) {

    private static final int BCRYPT_MAX_BYTES = 72;

    // @Size conta caracteres, mas o limite do BCrypt é em bytes: letras acentuadas ocupam 2 bytes.
    @AssertTrue(message = "A nova senha é longa demais. Letras acentuadas ocupam mais espaço; use menos caracteres.")
    public boolean isNewPasswordWithinBcryptLimit() {
        return newPassword == null || newPassword.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }
}
