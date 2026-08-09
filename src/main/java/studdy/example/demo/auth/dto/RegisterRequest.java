package studdy.example.demo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Nome obrigatório.")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
    String name,

    @NotBlank(message = "Email obrigatório.")
    @Email(message = "Email inválido.")
    String email,

    @NotBlank(message = "Senha obrigatória.")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
    String password

){



}