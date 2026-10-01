package studdy.example.demo.auth.recovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.time.Clock;

@Configuration
public class RecoveryConfiguration {
    @Bean
    Clock recoveryClock() {
        return Clock.systemUTC();
    }

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
