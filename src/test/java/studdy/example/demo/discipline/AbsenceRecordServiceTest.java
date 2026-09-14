package studdy.example.demo.discipline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.discipline.dto.AbsenceRecordResponse;
import studdy.example.demo.discipline.dto.CreateAbsenceRecordRequest;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

@SpringBootTest
@Transactional
class AbsenceRecordServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private FrequencyRepository frequencyRepository;

    @Autowired
    private AbsenceRecordService absenceRecordService;

    private AppUser owner;
    private Dashboard dashboard;
    private Discipline discipline;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new AppUser("Dono", "historico-frequencia@example.com", "hash"));
        dashboard = dashboardRepository.save(new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, owner));
        discipline = disciplineRepository.save(new Discipline(
                "Cálculo",
                "Professora Ana",
                "#4F46E5",
                new BigDecimal("6.00"),
                new BigDecimal("75.00"),
                dashboard,
                List.of(),
                "2",
                "2026"
        ));
    }

    @Test
    void savesTheRecordAndUpdatesTheFrequency() {
        AbsenceRecordResponse created = absenceRecordService.create(
                owner.getId(), dashboard.getId(), discipline.getId(),
                new CreateAbsenceRecordRequest(
                        LocalDate.of(2026, 9, 14), 1, "Saúde", "Consulta médica"
                )
        );

        assertEquals("Cálculo", created.disciplineName());
        assertEquals(new BigDecimal("5.00"), created.impact());
        assertEquals(1, frequencyRepository.findByDiscipline_Id(discipline.getId()).orElseThrow().getAbsences());

        List<AbsenceRecordResponse> history = absenceRecordService.findAll(
                owner.getId(), dashboard.getId(), discipline.getId()
        );
        assertEquals(1, history.size());
        assertEquals(created.id(), history.getFirst().id());
        assertEquals("Consulta médica", history.getFirst().note());
    }

    @Test
    void deletingARecordRemovesItsAbsencesFromTheTotal() {
        AbsenceRecordResponse created = absenceRecordService.create(
                owner.getId(), dashboard.getId(), discipline.getId(),
                new CreateAbsenceRecordRequest(LocalDate.now(), 2, "Pessoal", "")
        );

        absenceRecordService.delete(owner.getId(), dashboard.getId(), discipline.getId(), created.id());

        assertTrue(absenceRecordService.findAll(owner.getId(), dashboard.getId(), discipline.getId()).isEmpty());
        assertEquals(0, frequencyRepository.findByDiscipline_Id(discipline.getId()).orElseThrow().getAbsences());
    }
}
