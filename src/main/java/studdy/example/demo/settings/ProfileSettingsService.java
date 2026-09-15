package studdy.example.demo.settings;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.auth.dto.AuthResponse;
import studdy.example.demo.security.JwtService;
import studdy.example.demo.settings.dto.ChangePasswordRequest;
import studdy.example.demo.settings.dto.ProfileResponse;
import studdy.example.demo.settings.dto.UpdateProfileRequest;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.util.Locale;
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
    public ProfileResponse update(UUID userId, UpdateProfileRequest request) {
        AppUser user = accountCredentials.findUser(userId);
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // O e-mail é o login: trocá-lo exige a senha, para que um token vazado não sequestre a conta.
        if (!email.equals(user.getEmail())) {
            accountCredentials.requireCurrentPassword(user, request.currentPassword());

            if (userRepository.existsByEmail(email)) {
                throw emailInUse();
            }
        }

        user.updateProfile(request.name().trim(), email);

        try {
            return ProfileResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            // Outra conta pegou o mesmo e-mail entre a checagem e a gravação.
            throw emailInUse();
        }
    }

    @Transactional
    public AuthResponse changePassword(UUID userId, ChangePasswordRequest request) {
        AppUser user = accountCredentials.findUser(userId);
        accountCredentials.requireCurrentPassword(user, request.currentPassword());

        // A senha atual já foi conferida acima, então comparar o texto evita um segundo BCrypt.
        if (request.newPassword().equals(request.currentPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nova senha deve ser diferente da atual.");
        }

        user.changePasswordHash(accountCredentials.encode(request.newPassword()));
        userRepository.save(user);

        // Emitido no instante registrado da troca: os tokens anteriores são recusados e este mantém
        // a sessão atual aberta.
        return new AuthResponse(
                jwtService.generateToken(user.getId(), user.getCredentialsUpdatedAt()),
                "Bearer",
                jwtService.getExpirationInSeconds()
        );
    }

    private ResponseStatusException emailInUse() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com este e-mail.");
    }
}
