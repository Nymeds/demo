package studdy.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import studdy.example.demo.auth.session.BrowserSessionService;

/** Remove diariamente as sessões de navegador expiradas (cron em app.session.purge-cron). */
@Configuration
@EnableScheduling
public class SessionPurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionPurgeScheduler.class);

    private final BrowserSessionService sessions;

    public SessionPurgeScheduler(BrowserSessionService sessions) {
        this.sessions = sessions;
    }

    @Scheduled(cron = "${app.session.purge-cron:0 30 3 * * *}")
    public void purgeExpiredSessions() {
        try {
            int removed = sessions.purgeExpired();
            log.info("Limpeza de sessões: {} sessão(ões) expirada(s) removida(s).", removed);
        } catch (RuntimeException exception) {
            log.error("Falha ao remover sessões expiradas; nova tentativa no próximo agendamento.", exception);
        }
    }
}
