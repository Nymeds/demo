package studdy.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.user.UserRepository;

@SpringBootTest
@ActiveProfiles("seed")
class DemoDataSeedSeedProfileTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DemoDataSeed seed;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Test
    void seedProfileActivatesTheSeedBeanAndCreatesTheDemoUser() {
        assertThat(context.getBeansOfType(DemoDataSeed.class)).hasSize(1);
        assertThat(userRepository.existsByEmail(DemoDataSeed.DEVELOPER_EMAIL)).isTrue();
    }

    @Test
    void runningTheSeedAgainDoesNotDuplicateData() throws Exception {
        var developer = userRepository.findByEmail(DemoDataSeed.DEVELOPER_EMAIL).orElseThrow();
        long usersBefore = userRepository.count();
        long dashboardsBefore = dashboardRepository.count();
        long disciplinesBefore = disciplineRepository.count();

        seed.run(new DefaultApplicationArguments());

        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(dashboardRepository.count()).isEqualTo(dashboardsBefore);
        assertThat(disciplineRepository.count()).isEqualTo(disciplinesBefore);
        assertThat(dashboardRepository.findAllByOwner_IdOrderByNameAsc(developer.getId())).hasSize(1);
    }
}
