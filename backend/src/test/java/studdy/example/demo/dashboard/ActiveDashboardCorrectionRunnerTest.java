package studdy.example.demo.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActiveDashboardCorrectionRunnerTest {

    @Test
    void neverAbortsStartupWhenTheCorrectionFails() {
        ActiveDashboardCorrectionService service = mock(ActiveDashboardCorrectionService.class);
        when(service.correct()).thenThrow(new IllegalStateException("banco indisponivel"));

        assertDoesNotThrow(() -> new ActiveDashboardCorrectionRunner(service).run(null));
    }

    @Test
    void declaresAnExplicitOrder() {
        assertNotNull(ActiveDashboardCorrectionRunner.class.getAnnotation(Order.class));
    }
}
