package studdy.example.demo.auth;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh token inválido, expirado ou revogado.");
    }
}
