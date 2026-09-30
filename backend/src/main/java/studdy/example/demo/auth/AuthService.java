package studdy.example.demo.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.auth.dto.AuthResponse;
import studdy.example.demo.auth.dto.LoginRequest;
import studdy.example.demo.auth.dto.RegisterRequest;
import studdy.example.demo.legal.LegalVersions;
import studdy.example.demo.security.PasswordLimits;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;
import studdy.example.demo.user.dto.UserResponse;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private static final String DUPLICATE_EMAIL = "Já existe um usuário com este e-mail.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final RegistrationLimiter registrationLimiter;
    private final RefreshTokenService refreshTokenService;
    private final SessionTokens sessionTokens;
    private final LegalVersions legalVersions;
    private final Clock clock;
    // Hash calculado uma vez na inicialização. Quando o e-mail não existe, o BCrypt roda contra ele
    // para a resposta levar o mesmo tempo de uma senha errada (não revela quais e-mails existem).
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            LoginAttemptLimiter loginAttemptLimiter,
            RegistrationLimiter registrationLimiter,
            RefreshTokenService refreshTokenService,
            SessionTokens sessionTokens,
            LegalVersions legalVersions,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.registrationLimiter = registrationLimiter;
        this.refreshTokenService = refreshTokenService;
        this.sessionTokens = sessionTokens;
        this.legalVersions = legalVersions;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public UserResponse register(RegisterRequest request, String clientIp) {
        registrationLimiter.acquire(clientIp);
        String email = normalizeEmail(request.email());

        if (!legalVersions.termsVersion().equals(request.termsVersion().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Os Termos de Uso foram atualizados. Recarregue a página e leia a versão atual."
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, DUPLICATE_EMAIL);
        }

        AppUser user = new AppUser(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password())
        );

        user.acceptTerms(clock.instant(), legalVersions.termsVersion(), legalVersions.privacyVersion());

        try {
            // Dois cadastros simultâneos com o mesmo e-mail: a restrição única do banco decide.
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, DUPLICATE_EMAIL);
        }
    }

    // Sem @Transactional: o BCrypt não deve segurar uma conexão do banco.
    public AuthResponse login(LoginRequest request, String clientIp) {
        String email = normalizeEmail(request.email());
        loginAttemptLimiter.acquire(email, clientIp);

        // Mesma resposta (e custo) para e-mail inexistente, senha errada e senha acima de 72 bytes,
        // que nunca pode ser a senha da conta. A tentativa reservada fica contada como falha.
        AppUser user = userRepository.findByEmail(email).orElse(null);
        if (!passwordMatches(request.password(), user)) {
            throw invalidCredentials();
        }

        loginAttemptLimiter.recordSuccess(email, clientIp);
        return sessionTokens.login(user, request.shouldRemember());
    }

    // Sem @Transactional aqui: a revogação da família em caso de reuso precisa ser confirmada
    // pela transação do RefreshTokenService mesmo que a chamada termine em 401.
    public AuthResponse refresh(String refreshToken) {
        return sessionTokens.rotate(refreshToken);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private boolean passwordMatches(String rawPassword, AppUser user) {
        if (PasswordLimits.exceedsBcryptLimit(rawPassword)) {
            passwordEncoder.matches("", dummyPasswordHash);
            return false;
        }
        if (user == null) {
            passwordEncoder.matches(rawPassword, dummyPasswordHash);
            return false;
        }
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
    }
}
