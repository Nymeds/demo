package studdy.example.demo.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class ActiveDashboardCorrectionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ActiveDashboardCorrectionRunner.class);

    private final ActiveDashboardCorrectionService correctionService;

    public ActiveDashboardCorrectionRunner(ActiveDashboardCorrectionService correctionService) {
        this.correctionService = correctionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Correção de dados legados nunca pode impedir a aplicação de subir.
        try {
            ActiveDashboardCorrectionService.Result result = correctionService.correct();
            if (result.accountsAdjusted() > 0) {
                log.info("Correção de dashboards ativos: {} conta(s) ajustada(s), {} dashboard(s) desativado(s).",
                        result.accountsAdjusted(), result.dashboardsDeactivated());
            }
        } catch (RuntimeException exception) {
            log.error("Falha na correção de dashboards ativos; a inicialização continua.", exception);
        }
    }
}
