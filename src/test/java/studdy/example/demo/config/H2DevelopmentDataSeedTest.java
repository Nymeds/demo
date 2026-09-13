package studdy.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

@SpringBootTest
class H2DevelopmentDataSeedTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createsDeveloperAccountAndSampleAcademicData() {
        AppUser developer = userRepository.findByEmail(H2DevelopmentDataSeed.DEVELOPER_EMAIL)
                .orElseThrow();

        assertThat(passwordEncoder.matches(
                H2DevelopmentDataSeed.DEVELOPER_PASSWORD,
                developer.getPasswordHash()
        )).isTrue();

        var dashboards = dashboardRepository.findAllByOwner_IdOrderByNameAsc(developer.getId());
        assertThat(dashboards).hasSize(1);
        assertThat(disciplineRepository.findAllByDashboard_IdOrderByNameAsc(dashboards.getFirst().getId()))
                .hasSize(3);
    }
}
