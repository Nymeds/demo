package studdy.example.demo.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Sobe o contexto contra um PostgreSQL real (Flyway + Hibernate). Opt-in: so roda com RUN_POSTGRES_TESTS=true; sem Docker, e pulado (nao falha).
 */
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.baseline-version=1",
        "spring.flyway.locations=classpath:db/migration",
        "spring.datasource.driver-class-name=org.postgresql.Driver"
})
class PostgresSmokeTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void appliesMigrationsAndCreatesTables() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tables).contains("flyway_schema_history", "app_users", "dashboards", "disciplines",
                "browser_sessions").doesNotContain("refresh_tokens");
        Integer failed = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE NOT success", Integer.class);
        assertThat(failed).isZero();
    }
}
