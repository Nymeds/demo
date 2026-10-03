package studdy.example.demo.auth.recovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RecoveryMailerLifecycleTest {
    private static final String SECRET = "test-secret-with-at-least-32-bytes";
    private static final String URL = "http://127.0.0.1:3001";
    @TempDir Path directory;

    @Test
    void reusesAuthenticatedHealthyMailerWithoutStartingOrStoppingIt() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL + "/internal/health"))
                .andExpect(header("Authorization", "Bearer " + SECRET))
                .andRespond(withSuccess("{\"status\":\"ready\",\"smtpPort\":587}", MediaType.APPLICATION_JSON));
        var launches = new AtomicInteger();
        var lifecycle = new RecoveryMailerLifecycle(true, URL, SECRET, directory, "node", builder.build(), process -> {
            launches.incrementAndGet();
            throw new AssertionError("Não deve iniciar outro processo");
        });
        lifecycle.start();
        lifecycle.start();
        lifecycle.stop();
        assertThat(launches).hasValue(0);
        server.verify();
    }

    @Test
    void startsLocalNodeOnceChecksReadinessAndTerminatesOnlyOwnedProcess() throws Exception {
        Files.createDirectories(directory.resolve("src"));
        Files.createDirectories(directory.resolve("node_modules/nodemailer"));
        Files.createFile(directory.resolve(".env"));
        Files.createFile(directory.resolve("src/index.js"));
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL + "/internal/health")).andRespond(withServiceUnavailable());
        server.expect(requestTo(URL + "/internal/health"))
                .andExpect(header("Authorization", "Bearer " + SECRET))
                .andRespond(withSuccess("{\"status\":\"ready\",\"smtpPort\":465}", MediaType.APPLICATION_JSON));
        var process = mock(Process.class);
        when(process.isAlive()).thenReturn(true);
        when(process.waitFor(2, TimeUnit.SECONDS)).thenReturn(true);
        var launches = new AtomicInteger();
        var lifecycle = new RecoveryMailerLifecycle(true, URL, SECRET, directory, "custom-node", builder.build(), command -> {
            launches.incrementAndGet();
            assertThat(command.command()).containsExactly("custom-node", "--env-file=.env", "src/index.js");
            assertThat(command.directory().toPath()).isEqualTo(directory);
            assertThat(command.environment()).containsEntry("MAILER_API_SECRET", SECRET)
                    .containsEntry("MAILER_HOST", "127.0.0.1").containsEntry("MAILER_PORT", "3001");
            return process;
        });
        lifecycle.start();
        lifecycle.start();
        assertThat(launches).hasValue(1);
        lifecycle.stop();
        verify(process).destroy();
        verify(process, never()).destroyForcibly();
        server.verify();
    }

    @Test
    void doesNotRunWithDisabledAutoStartRemoteUrlOrMissingSecrets() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        RecoveryMailerLifecycle.ProcessLauncher launcher = command -> { throw new AssertionError("Não deve executar Node"); };
        var client = builder.build();
        new RecoveryMailerLifecycle(false, URL, SECRET, directory, "node", client, launcher).start();
        new RecoveryMailerLifecycle(true, "https://mailer.example.com", SECRET, directory, "node", client, launcher).start();
        new RecoveryMailerLifecycle(true, URL, "", directory, "node", client, launcher).start();
        server.verify();
    }

    @Test
    void missingDependenciesAndEarlyProcessExitAreHandledWithoutUnboundedWait() throws Exception {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL + "/internal/health")).andRespond(withServiceUnavailable());
        var lifecycle = new RecoveryMailerLifecycle(true, URL, SECRET, directory, "node", builder.build(), command -> {
            throw new AssertionError("Dependências ausentes");
        });
        lifecycle.start();
        server.verify();

        Files.createDirectories(directory.resolve("src"));
        Files.createDirectories(directory.resolve("node_modules/nodemailer"));
        Files.createFile(directory.resolve(".env"));
        Files.createFile(directory.resolve("src/index.js"));
        var process = mock(Process.class);
        when(process.waitFor(250, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(process.exitValue()).thenReturn(1);
        var secondBuilder = RestClient.builder();
        var secondServer = MockRestServiceServer.bindTo(secondBuilder).build();
        secondServer.expect(requestTo(URL + "/internal/health")).andRespond(withServiceUnavailable());
        new RecoveryMailerLifecycle(true, URL, SECRET, directory, "node", secondBuilder.build(), command -> process).start();
        verify(process).exitValue();
        secondServer.verify();
    }
}
