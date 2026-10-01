package studdy.example.demo.auth.recovery;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/password-recovery")
public class PasswordRecoveryController {
    private final PasswordRecoveryService service;
    private final RecoveryRateLimiter limiter;
    private final RecoveryRequestDispatcher dispatcher;

    public PasswordRecoveryController(PasswordRecoveryService service, RecoveryRateLimiter limiter,
                                      RecoveryRequestDispatcher dispatcher) {
        this.service = service;
        this.limiter = limiter;
        this.dispatcher = dispatcher;
    }

    @PostMapping("/request")
    public ResponseEntity<Message> request(@Valid @RequestBody EmailRequest body, HttpServletRequest request) {
        limiter.check(request.getRemoteAddr());
        dispatcher.enqueue(body.email());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(new Message(
                "Se este e-mail estiver cadastrado, você receberá um código. Confira também a pasta de spam."));
    }

    @PostMapping("/verify")
    public ResponseEntity<Verification> verify(@Valid @RequestBody CodeRequest body, HttpServletRequest request) {
        limiter.check(request.getRemoteAddr());
        String token = service.verifyCode(body.email(), body.code());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Verification(token, 300));
    }

    @PostMapping("/reset")
    public ResponseEntity<Message> reset(@Valid @RequestBody ResetRequest body, HttpServletRequest request) {
        limiter.check(request.getRemoteAddr());
        service.resetPassword(body.email(), body.resetToken(), body.newPassword());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Message(
                "Senha alterada com sucesso. Entre novamente com sua nova senha."));
    }

    public record EmailRequest(@NotBlank @Email @Size(max = 150) String email) {}
    public record CodeRequest(@NotBlank @Email @Size(max = 150) String email,
                              @NotBlank @Pattern(regexp = "[0-9]{6}", message = "Informe o código de 6 dígitos.") String code) {}
    public record ResetRequest(@NotBlank @Email @Size(max = 150) String email,
                               @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String resetToken,
                               @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.") String newPassword) {}
    public record Message(String message) {}
    public record Verification(String resetToken, int expiresIn) {}
}
