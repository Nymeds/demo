package studdy.example.demo.auth.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@Component
public class RecoveryMailSender {
    private static final Logger log = LoggerFactory.getLogger(RecoveryMailSender.class);
    private final TaskExecutor executor;
    private final URI endpoint;
    private final String secret;
    private final RestClient client;

    @Autowired
    public RecoveryMailSender(@Qualifier("recoveryMailExecutor") TaskExecutor executor,
                              @Value("${app.recovery.mailer-url}") String url,
                              @Value("${app.recovery.mailer-secret}") String secret) {
        this(executor, url, secret, createClient());
    }

    RecoveryMailSender(TaskExecutor executor, String url, String secret, RestClient client) {
        this.executor = executor;
        this.endpoint = URI.create(url + "/internal/recovery-email");
        this.secret = secret;
        if (!"https".equals(endpoint.getScheme()) && !("http".equals(endpoint.getScheme())
                && ("127.0.0.1".equals(endpoint.getHost()) || "localhost".equals(endpoint.getHost())))) {
            throw new IllegalArgumentException("MAILER_URL exige HTTPS fora de localhost.");
        }
        this.client = client;
    }

    private static RestClient createClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder().requestFactory(factory).build();
    }

    public void sendAfterCommit(String email, String code) {
        // SMTP não afeta o tempo nem a resposta pública e só recebe códigos já persistidos.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    executor.execute(() -> deliver(email, code));
                } catch (RuntimeException exception) {
                    log.error("Fila de e-mails de recuperação indisponível.");
                }
            }
        });
    }

    private void deliver(String email, String code) {
        try {
            if (secret.length() < 32) throw new IllegalStateException("Mailer não configurado");
            var response = client.post().uri(endpoint)
                    .header("Authorization", "Bearer " + secret)
                    .header("Content-Type", "application/json")
                    .body(Map.of("email", email, "code", code))
                    .retrieve().toBodilessEntity();
            int status = response.getStatusCode().value();
            if (status != 204) log.error("Serviço de e-mail recusou envio de recuperação (HTTP {}).", status);
        } catch (Exception exception) {
            log.error("Falha ao enviar recuperação. Verifique serviço de e-mail e configuração SMTP.");
        }
    }
}
