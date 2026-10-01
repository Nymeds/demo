package studdy.example.demo.auth.recovery;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** Inicia somente o mailer local e encerra somente o processo criado por esta API. */
@Component
public class RecoveryMailerLifecycle {
    private static final Logger log = LoggerFactory.getLogger(RecoveryMailerLifecycle.class);
    private final boolean enabled;
    private final URI baseUrl;
    private final String secret;
    private final Path directory;
    private final String node;
    private final RestClient client;
    private final ProcessLauncher launcher;
    private Process child;
    private boolean initialized;

    @Autowired
    public RecoveryMailerLifecycle(@Value("${app.recovery.mailer-auto-start}") boolean enabled,
                                   @Value("${app.recovery.mailer-url}") String url,
                                   @Value("${app.recovery.mailer-secret}") String secret,
                                   @Value("${app.recovery.mailer-directory}") String directory,
                                   @Value("${app.recovery.node-executable}") String node) {
        this(enabled, url, secret, Path.of(directory), node, createClient(), ProcessBuilder::start);
    }

    RecoveryMailerLifecycle(boolean enabled, String url, String secret, Path directory,
                            String node, RestClient client, ProcessLauncher launcher) {
        this.enabled = enabled;
        this.baseUrl = URI.create(url);
        this.secret = secret;
        this.directory = directory.toAbsolutePath().normalize();
        this.node = node;
        this.client = client;
        this.launcher = launcher;
    }

    private static RestClient createClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(1));
        return RestClient.builder().requestFactory(factory).build();
    }

    @EventListener
    public void onServerStarted(WebServerInitializedEvent event) {
        if (event.getApplicationContext().getServerNamespace() == null) start();
    }

    synchronized void start() {
        if (!enabled || initialized) return;
        initialized = true;
        if (!"http".equals(baseUrl.getScheme())
                || !("127.0.0.1".equals(baseUrl.getHost()) || "localhost".equals(baseUrl.getHost()))
                || baseUrl.getPort() < 1 || baseUrl.getUserInfo() != null
                || baseUrl.getQuery() != null || baseUrl.getFragment() != null
                || !(baseUrl.getPath().isEmpty() || "/".equals(baseUrl.getPath()))) {
            log.error("Inicialização automática exige MAILER_URL local com porta explícita. Para serviço externo, use MAILER_AUTO_START=false.");
            return;
        }
        if (secret.length() < 32) {
            log.error("Mailer não iniciado: execute node scripts/setup-recovery.mjs e configure mailer/.env.");
            return;
        }
        if (isReady()) {
            log.info("Serviço Nodemailer já disponível; reutilizando processo existente.");
            return;
        }
        if (!Files.isRegularFile(directory.resolve(".env"))
                || !Files.isRegularFile(directory.resolve("src/index.js"))
                || !Files.isDirectory(directory.resolve("node_modules/nodemailer"))) {
            log.error("Mailer não iniciado: configure mailer/.env e execute npm ci na pasta mailer.");
            return;
        }
        try {
            var builder = new ProcessBuilder(node, "--env-file=.env", "src/index.js")
                    .directory(directory.toFile()).inheritIO();
            // A configuração carregada pela API prevalece sobre divergências no arquivo Node.
            builder.environment().put("MAILER_API_SECRET", secret);
            builder.environment().put("MAILER_HOST", "127.0.0.1");
            builder.environment().put("MAILER_PORT", Integer.toString(baseUrl.getPort()));
            child = launcher.start(builder);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
            while (System.nanoTime() < deadline) {
                if (child.waitFor(250, TimeUnit.MILLISECONDS)) {
                    log.error("Nodemailer encerrou na inicialização (status {}). Confira os diagnósticos SMTP acima.", child.exitValue());
                    return;
                }
                if (isReady()) {
                    log.info("Nodemailer iniciado e pronto para recuperação de senha.");
                    return;
                }
            }
            stop();
            log.error("Nodemailer não ficou pronto em 45 segundos. Confira rede, portas SMTP e senha de app.");
        } catch (IOException exception) {
            log.error("Não foi possível iniciar Node.js. Instale Node 20.19+ ou configure NODE_EXECUTABLE.");
        } catch (InterruptedException exception) {
            stop();
            Thread.currentThread().interrupt();
        }
    }

    private boolean isReady() {
        try {
            var health = client.get().uri(baseUrl.resolve("/internal/health"))
                    .header("Authorization", "Bearer " + secret).retrieve().body(MailerHealth.class);
            return health != null && "ready".equals(health.status())
                    && (health.smtpPort() == 465 || health.smtpPort() == 587);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @PreDestroy
    synchronized void stop() {
        if (child == null || !child.isAlive()) return;
        child.destroy();
        try {
            if (!child.waitFor(2, TimeUnit.SECONDS)) child.destroyForcibly();
        } catch (InterruptedException exception) {
            child.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    record MailerHealth(String status, int smtpPort) {}

    @FunctionalInterface
    interface ProcessLauncher {
        Process start(ProcessBuilder builder) throws IOException;
    }
}
