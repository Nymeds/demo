package studdy.example.demo.auth.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RecoveryRequestDispatcher {
    private static final Logger log = LoggerFactory.getLogger(RecoveryRequestDispatcher.class);
    private final TaskExecutor executor;
    private final PasswordRecoveryService service;

    public RecoveryRequestDispatcher(@Qualifier("recoveryRequestExecutor") TaskExecutor executor,
                                     PasswordRecoveryService service) {
        this.executor = executor;
        this.service = service;
    }

    public void enqueue(String email) {
        // Até a busca da conta ocorre em segundo plano: a resposta não depende de sua existência.
        try {
            executor.execute(() -> {
                try {
                    service.requestCode(email);
                } catch (RuntimeException exception) {
                    log.error("Falha ao preparar recuperação. Verifique disponibilidade do banco.");
                }
            });
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O serviço de recuperação está ocupado. Aguarde um minuto e tente novamente.");
        }
    }
}
