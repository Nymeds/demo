package studdy.example.demo.auth;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.auth.dto.AuthResponse;
import studdy.example.demo.security.JwtService;
import studdy.example.demo.user.AppUser;

import java.time.Instant;
import java.util.UUID;

// Monta o par access token + refresh token devolvido ao cliente.
@Component
public class SessionTokens {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public SessionTokens(JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponse login(AppUser user, boolean rememberMe) {
        RefreshTokenService.IssuedToken refresh = refreshTokenService.issue(user, rememberMe);
        return response(user.getId(), user.getCredentialsUpdatedAt(), refresh.token(), refresh.expiresAt());
    }

    public AuthResponse rotate(String rawRefreshToken) {
        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(rawRefreshToken);
        return response(rotated.userId(), rotated.credentialsUpdatedAt(), rotated.token(), rotated.expiresAt());
    }

    /**
     * Depois de trocar senha ou e-mail: todas as sessões caem e a atual recebe um par novo (refresh
     * de sessão curta), para não ser derrubada quando o access token vencer. O chamador deve ter
     * travado o usuário (UserRepository.findByIdForUpdate) na mesma transação.
     */
    @Transactional
    public AuthResponse reissueAfterCredentialChange(AppUser user) {
        refreshTokenService.revokeAllForUser(user.getId());
        RefreshTokenService.IssuedToken refresh = refreshTokenService.issue(user, false);
        return response(user.getId(), user.getCredentialsUpdatedAt(), refresh.token(), refresh.expiresAt());
    }

    private AuthResponse response(UUID userId, Instant credentialsUpdatedAt, String refreshToken, Instant refreshExpiresAt) {
        return new AuthResponse(
                jwtService.generateTokenFor(userId, credentialsUpdatedAt),
                "Bearer",
                jwtService.getExpirationInSeconds(),
                refreshToken,
                refreshExpiresAt
        );
    }
}
