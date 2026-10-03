package studdy.example.demo.config;

import java.math.BigDecimal;
import java.net.URI;
import java.sql.Connection;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import studdy.example.demo.activities.Activity;
import studdy.example.demo.activities.ActivityRepository;
import studdy.example.demo.activities.ActivityStatus;
import studdy.example.demo.activities.ActivityType;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.discipline.ClassSchedule;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.discipline.Frequency;
import studdy.example.demo.discipline.FrequencyRepository;
import studdy.example.demo.grade.Grade;
import studdy.example.demo.grade.GradeRepository;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

/**
 * Cria o usuario demo com dados academicos de exemplo. So existe nos perfis {@code dev} e {@code seed}.
 *
 * <ul>
 *   <li>{@code dev}: apenas em H2 em memoria (nunca toca um banco real, mesmo se o perfil for ligado por engano).</li>
 *   <li>{@code seed}: ativado explicitamente pelo pipeline de init, tambem em PostgreSQL.</li>
 * </ul>
 *
 * Idempotente: se o usuario demo ja existe, nada e criado. Nunca roda no perfil padrao.
 */
@Component
@Profile({"dev", "seed"})
public class DemoDataSeed implements ApplicationRunner {

    public static final String SEED_PROFILE = "seed";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeed.class);

    public static final String DEVELOPER_EMAIL = "desenvolvedor@dev.com";
    public static final String DEVELOPER_PASSWORD = "desenvolvedor@dev.com";

    private final DataSource dataSource;
    private final Environment environment;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final DashboardRepository dashboardRepository;
    private final DisciplineRepository disciplineRepository;
    private final ActivityRepository activityRepository;
    private final GradeRepository gradeRepository;
    private final FrequencyRepository frequencyRepository;
    private final Clock clock;

    public DemoDataSeed(
            DataSource dataSource,
            Environment environment,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            DashboardRepository dashboardRepository,
            DisciplineRepository disciplineRepository,
            ActivityRepository activityRepository,
            GradeRepository gradeRepository,
            FrequencyRepository frequencyRepository,
            Clock clock
    ) {
        this.clock = clock;
        this.dataSource = dataSource;
        this.environment = environment;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.dashboardRepository = dashboardRepository;
        this.disciplineRepository = disciplineRepository;
        this.activityRepository = activityRepository;
        this.gradeRepository = gradeRepository;
        this.frequencyRepository = frequencyRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!isSeedAllowed()) {
            return;
        }
        if (userRepository.existsByEmail(DEVELOPER_EMAIL)) {
            log.info("Usuario demo {} ja existe; seed ignorado.", DEVELOPER_EMAIL);
            return;
        }

        log.warn("SEED DEMO: criando usuario demo {} com senha conhecida. Nunca use os perfis dev/seed em producao.",
                DEVELOPER_EMAIL);
        AppUser developer = userRepository.save(new AppUser(
                "Desenvolvedor",
                DEVELOPER_EMAIL,
                passwordEncoder.encode(DEVELOPER_PASSWORD)
        ));

        Dashboard dashboard = dashboardRepository.save(new Dashboard(
                "Meu semestre",
                DashboardStatus.ACTIVE,
                developer
        ));

        Discipline algorithms = disciplineRepository.save(new Discipline(
                "Algoritmos e Estruturas de Dados",
                "Profa. Marina Costa",
                "#6D4AFF",
                new BigDecimal("7.00"),
                new BigDecimal("75.00"),
                dashboard,
                List.of(
                        new ClassSchedule(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(20, 40)),
                        new ClassSchedule(DayOfWeek.WEDNESDAY, LocalTime.of(19, 0), LocalTime.of(20, 40))
                ),
                "2026.2",
                "2"
        ));

        Discipline databases = disciplineRepository.save(new Discipline(
                "Banco de Dados",
                "Prof. Rafael Almeida",
                "#19B36B",
                new BigDecimal("6.00"),
                new BigDecimal("75.00"),
                dashboard,
                List.of(new ClassSchedule(
                        DayOfWeek.TUESDAY,
                        LocalTime.of(20, 50),
                        LocalTime.of(22, 30)
                ))
                ,
                "2026.2",
                "2"
        ));

        Discipline ux = disciplineRepository.save(new Discipline(
                "Interação Humano-Computador",
                "Profa. Camila Santos",
                "#F28C28",
                new BigDecimal("7.00"),
                new BigDecimal("70.00"),
                dashboard,
                List.of(new ClassSchedule(
                        DayOfWeek.THURSDAY,
                        LocalTime.of(19, 0),
                        LocalTime.of(22, 30)
                ))
                ,
                "2026.2",
                "2"
        ));

        LocalDate today = LocalDate.now(clock);
        activityRepository.saveAll(List.of(
                new Activity(
                        "Lista de árvores binárias",
                        "Resolver os exercícios 1 a 10 e enviar o código-fonte.",
                        today.plusDays(3),
                        ActivityStatus.IN_PROGRESS,
                        ActivityType.ACTIVITY,
                        algorithms
                ),
                new Activity(
                        "Prova de estruturas de dados",
                        "Conteúdo: árvores, filas e pilhas.",
                        today.plusDays(10),
                        ActivityStatus.PENDING,
                        ActivityType.EXAM,
                        algorithms
                ),
                new Activity(
                        "Modelagem do banco do projeto",
                        "Finalizar o modelo lógico e revisar os relacionamentos.",
                        today.plusDays(6),
                        ActivityStatus.PENDING,
                        ActivityType.ACTIVITY,
                        databases
                ),
                new Activity(
                        "Protótipo navegável",
                        "Aplicar os ajustes encontrados na avaliação de usabilidade.",
                        today.plusDays(9),
                        ActivityStatus.PENDING,
                        ActivityType.ACTIVITY,
                        ux
                ),
                new Activity(
                        "Mapa de jornada do estudante",
                        "Entrega concluída para demonstração dos indicadores.",
                        today.minusDays(2),
                        ActivityStatus.COMPLETED,
                        ActivityType.ACTIVITY,
                        ux
                )
        ));

        gradeRepository.saveAll(List.of(
                new Grade(algorithms, "Avaliação 1", new BigDecimal("8.50"), today.minusDays(18)),
                new Grade(databases, "Trabalho prático", new BigDecimal("9.00"), today.minusDays(12)),
                new Grade(ux, "Pesquisa com usuários", new BigDecimal("8.00"), today.minusDays(7))
        ));

        frequencyRepository.saveAll(List.of(
                new Frequency(algorithms, 2),
                new Frequency(databases, 1),
                new Frequency(ux, 2)
        ));
    }

    public static final String ALLOW_REMOTE_PROPERTY = "app.seed.allow-remote";

    private boolean isSeedAllowed() throws Exception {
        String url = datasourceUrl();
        boolean h2 = url.startsWith("jdbc:h2:");
        if (!environment.acceptsProfiles(Profiles.of(SEED_PROFILE)) && !h2) {
            return false;
        }
        if (h2 || isLocalHost(url) || environment.getProperty(ALLOW_REMOTE_PROPERTY, Boolean.class, false)) {
            return true;
        }
        log.warn("SEED DEMO ignorado: o banco {} nao e local. Defina {}=true para permitir.",
                hostOf(url), ALLOW_REMOTE_PROPERTY);
        return false;
    }

    private String datasourceUrl() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            return url == null ? "" : url;
        }
    }

    static boolean isLocalHost(String jdbcUrl) {
        String host = hostOf(jdbcUrl);
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
    }

    private static String hostOf(String jdbcUrl) {
        try {
            String host = URI.create(jdbcUrl.startsWith("jdbc:") ? jdbcUrl.substring(5) : jdbcUrl).getHost();
            return host == null ? "" : host;
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }
}
