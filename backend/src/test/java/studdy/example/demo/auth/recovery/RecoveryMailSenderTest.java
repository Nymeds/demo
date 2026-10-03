package studdy.example.demo.auth.recovery;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RecoveryMailSenderTest {
    @AfterEach
    void cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sendsAuthenticatedJsonOnlyAfterTransactionCommits() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var sender = new RecoveryMailSender(Runnable::run, "http://127.0.0.1:3001",
                "internal-secret-with-at-least-32-bytes", builder.build());
        server.expect(requestTo("http://127.0.0.1:3001/internal/recovery-email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer internal-secret-with-at-least-32-bytes"))
                .andExpect(content().json("{\"email\":\"student@example.com\",\"code\":\"001234\"}"))
                .andRespond(withNoContent());
        TransactionSynchronizationManager.initSynchronization();
        sender.sendAfterCommit("student@example.com", "001234");
        assertThatThrownBy(server::verify).isInstanceOf(AssertionError.class);
        TransactionSynchronizationUtils.triggerAfterCommit();
        server.verify();
    }

    @Test
    void rollbackDoesNotSendAnUncommittedCode() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var sender = new RecoveryMailSender(Runnable::run, "http://127.0.0.1:3001",
                "internal-secret-with-at-least-32-bytes", builder.build());
        TransactionSynchronizationManager.initSynchronization();
        sender.sendAfterCommit("student@example.com", "001234");
        TransactionSynchronizationUtils.triggerAfterCompletion(1);
        server.verify();
    }

    @Test
    void refusesPlainHttpOutsideLoopback() {
        var builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build();
        assertThatThrownBy(() -> new RecoveryMailSender(Runnable::run, "http://mailer.example.com",
                "internal-secret-with-at-least-32-bytes", builder.build()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
