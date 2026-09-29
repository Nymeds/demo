package studdy.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class DevSeedProfileTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void seedBeanIsAbsentWithoutDevProfile() {
        assertThat(context.getBeansOfType(H2DevelopmentDataSeed.class)).isEmpty();
    }
}
