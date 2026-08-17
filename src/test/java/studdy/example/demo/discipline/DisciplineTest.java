package studdy.example.demo.discipline;

import org.junit.jupiter.api.Test;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.user.AppUser;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DisciplineTest {

    private final Dashboard dashboard = new Dashboard(
            "Semestre 2026.2",
            DashboardStatus.ACTIVE,
            new AppUser("Estudante", "estudante@example.com", "hash")
    );

    @Test
    void normalizesThePassingAverageToTwoDecimalPlaces() {
        Discipline discipline = newDiscipline(new BigDecimal("7.5"));

        assertEquals(new BigDecimal("7.50"), discipline.getPassingAverage());
    }

    @Test
    void rejectsAPassingAverageOutsideOfTheAllowedRange() {
        assertThrows(IllegalArgumentException.class, () -> newDiscipline(new BigDecimal("10.01")));
        assertThrows(IllegalArgumentException.class, () -> newDiscipline(new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class, () -> newDiscipline(null));
    }

    @Test
    void updatesThePassingAverageOfAnExistingDiscipline() {
        Discipline discipline = newDiscipline(new BigDecimal("6.00"));

        discipline.update("Cálculo II", "Professora Ana", 60, new BigDecimal("8.00"), List.of());

        assertEquals(new BigDecimal("8.00"), discipline.getPassingAverage());
    }

    private Discipline newDiscipline(BigDecimal passingAverage) {
        return new Discipline("Cálculo", "Professora Ana", 60, passingAverage, dashboard, List.of());
    }
}
