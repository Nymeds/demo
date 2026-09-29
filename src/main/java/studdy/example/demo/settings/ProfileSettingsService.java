package studdy.example.demo.settings;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.auth.SessionTokens;
import studdy.example.demo.auth.dto.AuthResponse;
import studdy.example.demo.settings.dto.ChangePasswordRequest;
import studdy.example.demo.settings.dto.ProfileResponse;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.util.UUID;

@Service
public class ProfileSettingsService {

    private final UserRepository userRepository;
    private final AccountCredentials accountCredentials;
    private final SessionTokens sessionTokens;

    public ProfileSettingsService(
            UserRepository userRepository,
            AccountCredentials accountCredentials,
            SessionTokens sessionTokens
    ) {
        this.userRepository = userRepository;
        this.accountCredentials = accountCredentials;
        this.sessionTokens = sessionTokens;
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

        // Access tokens anteriores são recusados (credentialsUpdatedAt); todas as sessões caem e a
        // atual recebe um par novo.
        return sessionTokens.reissueAfterCredentialChange(user);
    }
}
