package studdy.example.demo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Token de sessão obrigatório.")
        String refreshToken
) {
}
