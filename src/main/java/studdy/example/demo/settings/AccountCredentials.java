package studdy.example.demo.settings;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

// Busca do usuário autenticado e confirmação da senha atual, compartilhadas pelas
// operações sensíveis das configurações (trocar e-mail, trocar senha, excluir conta).
@Component
class AccountCredentials {

    // O BCrypt aceita no máximo 72 bytes e lança exceção acima disso. Uma senha desse tamanho não
    // tem como ser a senha da conta, então é tratada como incorreta em vez de virar erro 500.
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    AccountCredentials(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    AppUser findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    void requireCurrentPassword(AppUser user, String currentPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe sua senha atual para confirmar.");
        }

        if (currentPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha atual está incorreta.");
        }
    }

    String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
