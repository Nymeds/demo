package studdy.example.demo.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import studdy.example.demo.security.MaxUtf8Bytes;

public record RegisterRequest(
    @NotBlank(message = "Nome obrigatório.")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
    String name,

    @NotBlank(message = "Email obrigatório.")
    @Email(message = "Email inválido.")
    @Size(max = 150, message = "O email deve ter no máximo 150 caracteres.")
    String email,

    @NotBlank(message = "Senha obrigatória.")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
    // @Size conta caracteres; o limite real do BCrypt é em bytes (acentos ocupam 2, emojis 4).
    @MaxUtf8Bytes
    String password,

    @NotNull(message = "É necessário aceitar os Termos de Uso e a Política de Privacidade.")
    @AssertTrue(message = "É necessário aceitar os Termos de Uso e a Política de Privacidade.")
    Boolean acceptedTerms,

    @NotBlank(message = "Informe a versão dos Termos de Uso aceita.")
    @Size(max = 40, message = "Versão dos Termos de Uso inválida.")
    String termsVersion
){
}
