package studdy.example.demo.discipline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Autowired
    private Clock clock;

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
    void rejectsATotalAboveTheCap() {
        for (int i = 0; i < 10; i++) {
            absenceRecordService.create(owner.getId(), dashboard.getId(), discipline.getId(),
                    new CreateAbsenceRecordRequest(LocalDate.of(2026, 9, 14), 999, "Saúde", null));
        }
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> absenceRecordService.create(owner.getId(), dashboard.getId(), discipline.getId(),
                        new CreateAbsenceRecordRequest(LocalDate.of(2026, 9, 14), 999, "Saúde", null)));
        assertEquals(400, error.getStatusCode().value());
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

    @Test
    void checksOwnershipBeforeValidatingTheDate() {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                absenceRecordService.create(
                        java.util.UUID.randomUUID(), dashboard.getId(), discipline.getId(),
                        new CreateAbsenceRecordRequest(tomorrow, 1, "Pessoal", "")
                )
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void rejectsRegisteringAnAbsenceWithAFutureDate() {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                absenceRecordService.create(
                        owner.getId(), dashboard.getId(), discipline.getId(),
                        new CreateAbsenceRecordRequest(tomorrow, 1, "Pessoal", "")
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Não é possível registrar falta em data futura.", exception.getReason());
        assertTrue(absenceRecordService.findAll(owner.getId(), dashboard.getId(), discipline.getId()).isEmpty());
    }

    @Test
    void allowsRegisteringAnAbsenceDatedToday() {
        LocalDate today = LocalDate.now(clock);

        AbsenceRecordResponse created = absenceRecordService.create(
                owner.getId(), dashboard.getId(), discipline.getId(),
                new CreateAbsenceRecordRequest(today, 1, "Pessoal", "")
        );

        assertEquals(today, created.date());
    }

    @Test
    void totalStaysConsistentWithHistoryAfterMultipleCreatesAndADelete() {
        AbsenceRecordResponse first = absenceRecordService.create(
                owner.getId(), dashboard.getId(), discipline.getId(),
                new CreateAbsenceRecordRequest(LocalDate.now(clock).minusDays(3), 2, "Saúde", "")
        );
        absenceRecordService.create(
                owner.getId(), dashboard.getId(), discipline.getId(),
                new CreateAbsenceRecordRequest(LocalDate.now(clock).minusDays(1), 3, "Pessoal", "")
        );

        int totalAfterCreates = frequencyRepository.findByDiscipline_Id(discipline.getId()).orElseThrow().getAbsences();
        int sumOfHistoryAfterCreates = absenceRecordService
                .findAll(owner.getId(), dashboard.getId(), discipline.getId())
                .stream()
                .mapToInt(AbsenceRecordResponse::quantity)
                .sum();
        assertEquals(sumOfHistoryAfterCreates, totalAfterCreates);
        assertEquals(5, totalAfterCreates);

        absenceRecordService.delete(owner.getId(), dashboard.getId(), discipline.getId(), first.id());

        int totalAfterDelete = frequencyRepository.findByDiscipline_Id(discipline.getId()).orElseThrow().getAbsences();
        int sumOfHistoryAfterDelete = absenceRecordService
                .findAll(owner.getId(), dashboard.getId(), discipline.getId())
                .stream()
                .mapToInt(AbsenceRecordResponse::quantity)
                .sum();
        assertEquals(sumOfHistoryAfterDelete, totalAfterDelete);
        assertEquals(3, totalAfterDelete);
    }

    // Nota sobre teste de concorrência: um teste real com duas threads/transações
    // concorrentes exigiria conexões e transações independentes da transação de
    // teste (a classe é @Transactional e faz rollback ao final), o que tornaria o
    // teste dependente de commit real no H2 em memória e sujeito a flakiness de
    // timing entre threads. A proteção de concorrência (lock pessimista em
    // Frequency, ver FrequencyRepository#findByDiscipline_IdForUpdate) é a mesma
    // usada por create/delete/update, então a consistência total-vs-histórico
    // coberta acima exercita o mesmo caminho de código que seria usado sob
    // concorrência; a serialização das transações concorrentes é garantida pelo
    // SELECT ... FOR UPDATE, verificável manualmente ou em teste de integração
    // com um datasource não transacional dedicado, caso necessário no futuro.
}
