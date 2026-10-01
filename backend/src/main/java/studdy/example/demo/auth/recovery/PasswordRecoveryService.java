package studdy.example.demo.auth.recovery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.user.UserRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class PasswordRecoveryService {
    private final UserRepository users;
    private final PasswordRecoveryRepository recoveries;
    private final PasswordEncoder encoder;
    private final RecoveryMailSender mail;
    private final Clock clock;
    private final byte[] hashSecret;
    private final SecureRandom random = new SecureRandom();

    public PasswordRecoveryService(UserRepository users, PasswordRecoveryRepository recoveries,
                                   PasswordEncoder encoder, RecoveryMailSender mail, Clock clock,
                                   @Value("${app.recovery.hash-secret}") String hashSecret) {
        if (hashSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("RECOVERY_HASH_SECRET deve ter pelo menos 32 bytes.");
        }
        this.users = users;
        this.recoveries = recoveries;
        this.encoder = encoder;
        this.mail = mail;
        this.clock = clock;
        this.hashSecret = hashSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public void requestCode(String email) {
        users.findByEmailForUpdate(normalize(email)).ifPresent(user -> {
            PasswordRecovery recovery = recoveries.findById(user.getId())
                    .orElseGet(() -> new PasswordRecovery(user));
            Instant now = now();
            if (!recovery.canSend(now)) return;
            String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
            UUID nonce = UUID.randomUUID();
            recovery.issue(nonce, hash("code:" + nonce + ":" + code), now);
            recoveries.save(recovery);
            mail.sendAfterCommit(user.getEmail(), code);
        });
    }

    // Tentativas erradas precisam ser confirmadas no banco, inclusive ao devolver HTTP 400.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public String verifyCode(String email, String code) {
        var user = users.findByEmailForUpdate(normalize(email)).orElseThrow(this::invalidCode);
        var recovery = recoveries.findById(user.getId()).orElseThrow(this::invalidCode);
        if (!recovery.canVerify(now())) throw invalidCode();
        if (!equal(recovery.getCodeHash(), hash("code:" + recovery.getNonce() + ":" + code))) {
            recovery.rejectCode();
            throw invalidCode();
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        recovery.authorizeReset(hash("token:" + token), now());
        return token;
    }

    @Transactional
    public void resetPassword(String email, String token, String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter no máximo 72 bytes UTF-8.");
        }
        var user = users.findByEmailForUpdate(normalize(email)).orElseThrow(this::invalidToken);
        var recovery = recoveries.findById(user.getId()).orElseThrow(this::invalidToken);
        if (!recovery.canReset(now()) || !equal(recovery.getTokenHash(), hash("token:" + token))) {
            throw invalidToken();
        }
        user.changePasswordHash(encoder.encode(password));
        recovery.consume();
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Instant now() {
        // Precisão comum a PostgreSQL e H2, sem arredondar limites ao persistir.
        return clock.instant().truncatedTo(ChronoUnit.MILLIS);
    }

    private String hash(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hashSecret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC indisponível", exception);
        }
    }

    private boolean equal(String first, String second) {
        return first != null && MessageDigest.isEqual(first.getBytes(StandardCharsets.UTF_8),
                second.getBytes(StandardCharsets.UTF_8));
    }

    private ResponseStatusException invalidCode() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Código inválido, expirado ou com tentativas esgotadas. Solicite outro código.");
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A autorização de recuperação expirou ou já foi usada. Solicite outro código.");
    }
}
