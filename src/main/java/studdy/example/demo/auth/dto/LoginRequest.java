package studdy.example.demo.auth.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest (
    @NotBlank(message = "Email obrigatório.")
    @Email(message = "Email inválido.")
    String email,

    @NotBlank(message = "Senha obrigatória.")
    String password,
    boolean rememberMe
){
    public LoginRequest(String email, String password) {
        this(email, password, false);
    }
}
