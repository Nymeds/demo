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

        discipline.update("Cálculo II", "Professora Ana", "#4F46E5", new BigDecimal("8.00"), new BigDecimal("75.0"), List.of());

        assertEquals(new BigDecimal("8.00"), discipline.getPassingAverage());
    }

    @Test
    void keepsThePassingAverageAndTheMinimumAttendanceApart() {
        // Os dois critérios convivem: nota vai de 0 a 10, frequência de 0 a 100.
        Discipline discipline = new Discipline(
                "Cálculo",
                "Professora Ana",
                "#4F46E5",
                new BigDecimal("6.00"),
                new BigDecimal("75.0"),
                dashboard,
                List.of()
        );

        assertEquals(new BigDecimal("6.00"), discipline.getPassingAverage());
        assertEquals(new BigDecimal("75.00"), discipline.getMinimumAttendancePercentage());
    }

    @Test
    void rejectsAMinimumAttendanceOutsideOfTheAllowedRange() {
        assertThrows(IllegalArgumentException.class, () -> newDiscipline(new BigDecimal("6.00"), new BigDecimal("100.01")));
        assertThrows(IllegalArgumentException.class, () -> newDiscipline(new BigDecimal("6.00"), new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class, () -> newDiscipline(new BigDecimal("6.00"), null));
    }

    @Test
    void rejectsTheTwoCriteriaSwappedByMistake() {
        // Trocar a ordem dos dois BigDecimal do construtor compila, mas não passa daqui:
        // 75 não é média válida. É a diferença de faixa que protege a chamada.
        assertThrows(
                IllegalArgumentException.class,
                () -> newDiscipline(new BigDecimal("75.00"), new BigDecimal("6.00"))
        );
    }

    private Discipline newDiscipline(BigDecimal passingAverage) {
        return newDiscipline(passingAverage, new BigDecimal("75.0"));
    }

    private Discipline newDiscipline(BigDecimal passingAverage, BigDecimal minimumAttendancePercentage) {
        return new Discipline(
                "Cálculo",
                "Professora Ana",
                "#4F46E5",
                passingAverage,
                minimumAttendancePercentage,
                dashboard,
                List.of()
        );
    }
}
