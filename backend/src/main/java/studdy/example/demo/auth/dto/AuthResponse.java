package studdy.example.demo.auth.dto;

// Só o access token (JWT curto). A sessão longa fica no cookie HttpOnly (auth/session/SessionCookies).
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
