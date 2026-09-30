package studdy.example.demo.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

import studdy.example.demo.activities.ActivityRepository;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.discipline.FrequencyRepository;
import studdy.example.demo.grade.GradeRepository;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

/** Regras de guarda do seed sem subir o contexto: quando ele pode escrever em um banco que nao e H2. */
class DemoDataSeedGuardTest {

    private final UserRepository userRepository = mock(UserRepository.class);

    private DemoDataSeed seedFor(String jdbcUrl, String... activeProfiles) throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getURL()).thenReturn(jdbcUrl);

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(activeProfiles);

        return new DemoDataSeed(
                dataSource,
                environment,
                mock(PasswordEncoder.class),
                userRepository,
                mock(DashboardRepository.class),
                mock(DisciplineRepository.class),
                mock(ActivityRepository.class),
                mock(GradeRepository.class),
                mock(FrequencyRepository.class)
        );
    }

    @Test
    void devProfileNeverSeedsARealPostgresDatabase() throws Exception {
        seedFor("jdbc:postgresql://localhost:5432/academic_organizer", "dev")
                .run(new DefaultApplicationArguments());

        verifyNoInteractions(userRepository);
    }

    @Test
    void seedProfileSkipsPostgresWhenTheDemoUserAlreadyExists() throws Exception {
        when(userRepository.existsByEmail(DemoDataSeed.DEVELOPER_EMAIL)).thenReturn(true);

        seedFor("jdbc:postgresql://localhost:5432/academic_organizer", "seed")
                .run(new DefaultApplicationArguments());

        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void seedProfileCreatesTheDemoUserInPostgresWhenMissing() throws Exception {
        when(userRepository.existsByEmail(DemoDataSeed.DEVELOPER_EMAIL)).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenThrow(new StopAfterFirstSave());

        try {
            seedFor("jdbc:postgresql://localhost:5432/academic_organizer", "seed")
                    .run(new DefaultApplicationArguments());
        } catch (StopAfterFirstSave expected) {
            // O seed chegou a gravar o usuario; o restante da carga e coberto pelos testes com H2.
        }

        verify(userRepository).save(any(AppUser.class));
    }

    private static final class StopAfterFirstSave extends RuntimeException {
    }
}
