package studdy.example.demo.config;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Remove a tabela legada {@code refresh_tokens} (substituída por browser_sessions) no PostgreSQL.
 * Idempotente e nunca interrompe a inicialização: uma falha só é registrada no log.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LegacyRefreshTokensCleanup implements ApplicationRunner {

    static final String DROP_SQL = "DROP TABLE IF EXISTS refresh_tokens CASCADE";
    private static final Logger log = LoggerFactory.getLogger(LegacyRefreshTokensCleanup.class);

    private final DataSource dataSource;

    public LegacyRefreshTokensCleanup(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("postgresql")) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(DROP_SQL);
            }
            log.info("Tabela legada refresh_tokens removida (se existia).");
        } catch (Exception exception) {
            log.error("Não foi possível remover a tabela legada refresh_tokens; a API segue iniciando.", exception);
        }
    }
}
