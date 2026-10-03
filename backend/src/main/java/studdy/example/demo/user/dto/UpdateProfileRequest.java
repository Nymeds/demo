package studdy.example.demo.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import studdy.example.demo.user.Gender;
import studdy.example.demo.user.ValidPhone;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String name,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
        String email,

        @Pattern(
                regexp = "^$|^[A-Za-z0-9](?:[A-Za-z0-9._]{1,28}[A-Za-z0-9])$",
                message = "O nome de usuário deve ter entre 3 e 30 caracteres e usar apenas letras, números, ponto ou sublinhado."
        )
        String username,

        @Size(max = 30, message = "Informe o celular completo com DDD: (DD) 9XXXX-XXXX.")
        @ValidPhone
        String phone,

        @Past(message = "A data de nascimento deve estar no passado.")
        LocalDate birthDate,

        Gender gender,

        @Size(max = 120, message = "A localização deve ter no máximo 120 caracteres.")
        String location,

        // Só é exigida quando o e-mail muda; a regra fica no serviço.
        @Size(max = 72, message = "A senha deve ter no máximo 72 caracteres.")
        String currentPassword
) {
    public UpdateProfileRequest {
        name = trim(name);
        email = trim(email);
        username = trim(username);
        phone = trim(phone);
        location = trim(location);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
