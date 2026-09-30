package studdy.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Tarefas periódicas (ex.: limpeza diária de refresh tokens vencidos).
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
