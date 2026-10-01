package studdy.example.demo.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class LegacyRefreshTokensCleanupTest {

    private final DataSource dataSource = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private final DatabaseMetaData metaData = mock(DatabaseMetaData.class);
    private final Statement statement = mock(Statement.class);

    private void database(String product) throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn(product);
        when(connection.createStatement()).thenReturn(statement);
    }

    @Test
    void dropsLegacyTableOnPostgres() throws Exception {
        database("PostgreSQL");
        new LegacyRefreshTokensCleanup(dataSource).run(new DefaultApplicationArguments());
        verify(statement).execute(LegacyRefreshTokensCleanup.DROP_SQL);
    }

    @Test
    void doesNothingOnH2() throws Exception {
        database("H2");
        new LegacyRefreshTokensCleanup(dataSource).run(new DefaultApplicationArguments());
        verify(connection, never()).createStatement();
    }

    @Test
    void neverAbortsStartupWhenDropFails() throws Exception {
        database("PostgreSQL");
        when(statement.execute(LegacyRefreshTokensCleanup.DROP_SQL)).thenThrow(new SQLException("sem permissão"));
        assertThatCode(() -> new LegacyRefreshTokensCleanup(dataSource).run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }
}
