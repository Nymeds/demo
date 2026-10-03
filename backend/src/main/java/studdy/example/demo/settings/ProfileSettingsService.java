package studdy.example.demo.settings;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.auth.dto.AuthResponse;
import studdy.example.demo.security.JwtService;
import studdy.example.demo.settings.dto.ChangePasswordRequest;
import studdy.example.demo.settings.dto.ProfileResponse;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.util.UUID;

@Service
public class ProfileSettingsService {

    private final UserRepository userRepository;
    private final AccountCredentials accountCredentials;
    private final JwtService jwtService;

    public ProfileSettingsService(
            UserRepository userRepository,
            AccountCredentials accountCredentials,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.accountCredentials = accountCredentials;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse find(UUID userId) {
        return ProfileResponse.from(accountCredentials.findUser(userId));
    }

    @Transactional
    public AuthResponse changePassword(UUID userId, ChangePasswordRequest request) {
        // Trava o usuário: uma renovação de sessão simultânea espera esta troca terminar.
        AppUser user = accountCredentials.findUserForUpdate(userId);
        accountCredentials.requireCurrentPassword(user, request.currentPassword());

        // A senha atual já foi conferida acima, então comparar o texto evita um segundo BCrypt.
        if (request.newPassword().equals(request.currentPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nova senha deve ser diferente da atual.");
        }

        user.changePasswordHash(accountCredentials.encode(request.newPassword()));
        userRepository.save(user);

        // Access tokens e sessões de navegador anteriores são recusados (credentialsUpdatedAt). Este
        // navegador recebe um token novo; o SettingsController troca o cookie da sessão dele.
        return new AuthResponse(
                jwtService.generateTokenFor(user.getId(), user.getCredentialsUpdatedAt()),
                "Bearer",
                jwtService.getExpirationInSeconds()
        );
    }
}
