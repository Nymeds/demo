package studdy.example.demo.auth.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest (
    @NotBlank(message = "Email obrigatório.")
    @Email(message = "Email inválido.")
    String email,

    @NotBlank(message = "Senha obrigatória.")
    String password,

    // Ausente no JSON equivale a false: sessão curta (12 h por padrão).
    Boolean rememberMe
){

    public boolean shouldRemember() {
        return Boolean.TRUE.equals(rememberMe);
    }
}
