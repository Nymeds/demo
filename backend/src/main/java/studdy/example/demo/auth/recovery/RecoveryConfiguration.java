package studdy.example.demo.auth.recovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// O Clock da recuperação e da sessão é o bean único de config/ClockConfig.
@Configuration
public class RecoveryConfiguration {
    @Bean
    ThreadPoolTaskExecutor recoveryMailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("recovery-mail-");
        return executor;
    }

    @Bean
    ThreadPoolTaskExecutor recoveryRequestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("recovery-request-");
        return executor;
    }
}
