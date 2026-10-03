package studdy.example.demo.auth.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest (
    @NotBlank(message = "Email obrigatório.")
    @Email(message = "Email inválido.")
    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
    String email,

    // Senhas acima de 72 bytes já são recusadas como credencial inválida (limite do BCrypt);
    // este teto só barra corpos enormes antes de qualquer trabalho.
    @NotBlank(message = "Senha obrigatória.")
    @Size(max = 200, message = "A senha deve ter no máximo 200 caracteres.")
    String password,

    // Ausente no JSON equivale a false: cookie de sessão do navegador, válido por até 12 h.
    Boolean rememberMe
){
    public LoginRequest(String email, String password) {
        this(email, password, false);
    }

    public boolean shouldRemember() {
        return Boolean.TRUE.equals(rememberMe);
    }
}
