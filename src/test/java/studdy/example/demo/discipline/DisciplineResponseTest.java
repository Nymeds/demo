package studdy.example.demo.discipline;

import org.junit.jupiter.api.Test;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.discipline.dto.DisciplineResponse;
import studdy.example.demo.grade.Grade;
import studdy.example.demo.user.AppUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisciplineResponseTest {

    private final AcademicPerformanceService performanceService = new AcademicPerformanceService();

    @Test
    void includesAverageAndStatusCalculatedFromRecordedGrades() {
        AppUser user = new AppUser("Estudante", "estudante@example.com", "hash");
        Dashboard dashboard = new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, user);
        Discipline discipline = new Discipline(
                "Cálculo",
                "Professora Ana",
                "#4F46E5",
                new BigDecimal("6.00"),
                new BigDecimal("75.0"),
                dashboard,
                List.of()
        );
        discipline.getGrades().add(new Grade(
                discipline,
                "Prova 1",
                new BigDecimal("5.50"),
                LocalDate.of(2026, 8, 15)
        ));

        DisciplineResponse response = DisciplineResponse.from(
                discipline,
                performanceService.calculate(discipline.getGrades(), discipline.getPassingAverage())
        );

        assertEquals(new BigDecimal("5.50"), response.average());
        assertEquals(new BigDecimal("6.00"), response.passingAverage());
        assertEquals(DisciplineStatus.FAILED_BY_GRADE, response.status());
    }
}
