package studdy.example.demo.settings;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.security.PasswordLimits;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.util.UUID;

// Busca do usuário autenticado e confirmação da senha atual, compartilhadas pelas
// operações sensíveis das configurações (trocar e-mail, trocar senha, excluir conta).
@Component
public class AccountCredentials {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordConfirmationLimiter confirmationLimiter;

    public AccountCredentials(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordConfirmationLimiter confirmationLimiter
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.confirmationLimiter = confirmationLimiter;
    }

    public AppUser findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(AccountCredentials::notFound);
    }

    /** Trava a linha do usuário até o fim da transação (troca de senha/e-mail x rotação de sessão). */
    public AppUser findUserForUpdate(UUID userId) {
        return userRepository.findByIdForUpdate(userId).orElseThrow(AccountCredentials::notFound);
    }

    /**
     * Confere a senha atual. Limitado por usuário (5 falhas a cada 15 minutos por padrão, 429).
     * O BCrypt aceita no máximo 72 bytes e lança exceção acima disso: uma senha desse tamanho não
     * tem como ser a senha da conta, então é tratada como incorreta em vez de virar erro 500.
     */
    public void requireCurrentPassword(AppUser user, String currentPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe sua senha atual para confirmar.");
        }

        confirmationLimiter.acquire(user.getId());

        if (PasswordLimits.exceedsBcryptLimit(currentPassword)
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha atual está incorreta.");
        }

        confirmationLimiter.recordSuccess(user.getId());
    }

    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
    }
}
