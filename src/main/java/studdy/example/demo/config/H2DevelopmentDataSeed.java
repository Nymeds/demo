package studdy.example.demo.config;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import studdy.example.demo.activities.Activity;
import studdy.example.demo.activities.ActivityRepository;
import studdy.example.demo.activities.ActivityStatus;
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

@Component
public class H2DevelopmentDataSeed implements ApplicationRunner {

    public static final String DEVELOPER_EMAIL = "desenvolvedor@dev.com";
    public static final String DEVELOPER_PASSWORD = "123";

    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final DashboardRepository dashboardRepository;
    private final DisciplineRepository disciplineRepository;
    private final ActivityRepository activityRepository;
    private final GradeRepository gradeRepository;
    private final FrequencyRepository frequencyRepository;

    public H2DevelopmentDataSeed(
            DataSource dataSource,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            DashboardRepository dashboardRepository,
            DisciplineRepository disciplineRepository,
            ActivityRepository activityRepository,
            GradeRepository gradeRepository,
            FrequencyRepository frequencyRepository
    ) {
        this.dataSource = dataSource;
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
        if (!isH2Database() || userRepository.existsByEmail(DEVELOPER_EMAIL)) {
            return;
        }

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
                )
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
        ));

        LocalDate today = LocalDate.now();
        activityRepository.saveAll(List.of(
                new Activity(
                        "Lista de árvores binárias",
                        "Resolver os exercícios 1 a 10 e enviar o código-fonte.",
                        today.plusDays(3),
                        ActivityStatus.IN_PROGRESS,
                        algorithms
                ),
                new Activity(
                        "Modelagem do banco do projeto",
                        "Finalizar o modelo lógico e revisar os relacionamentos.",
                        today.plusDays(6),
                        ActivityStatus.PENDING,
                        databases
                ),
                new Activity(
                        "Protótipo navegável",
                        "Aplicar os ajustes encontrados na avaliação de usabilidade.",
                        today.plusDays(9),
                        ActivityStatus.PENDING,
                        ux
                ),
                new Activity(
                        "Mapa de jornada do estudante",
                        "Entrega concluída para demonstração dos indicadores.",
                        today.minusDays(2),
                        ActivityStatus.COMPLETED,
                        ux
                )
        ));

        gradeRepository.saveAll(List.of(
                new Grade(algorithms, "Avaliação 1", new BigDecimal("8.50"), today.minusDays(18)),
                new Grade(databases, "Trabalho prático", new BigDecimal("9.00"), today.minusDays(12)),
                new Grade(ux, "Pesquisa com usuários", new BigDecimal("8.00"), today.minusDays(7))
        ));

        frequencyRepository.saveAll(List.of(
                new Frequency(algorithms, 24, 2),
                new Frequency(databases, 18, 1),
                new Frequency(ux, 20, 2)
        ));
    }

    private boolean isH2Database() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getURL().startsWith("jdbc:h2:");
        }
    }
}
