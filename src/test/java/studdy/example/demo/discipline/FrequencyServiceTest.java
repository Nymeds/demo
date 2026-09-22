package studdy.example.demo.discipline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.discipline.dto.CreateFrequencyRequest;
import studdy.example.demo.discipline.dto.FrequencyResponse;
import studdy.example.demo.discipline.dto.UpdateFrequencyRequest;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class FrequencyServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private FrequencyService frequencyService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DisciplineService disciplineService;

    private AppUser owner;
    private AppUser intruder;
    private Dashboard dashboard;
    private Discipline discipline;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new AppUser("Dono", "frequencia-dono@example.com", "hash"));
        intruder = userRepository.save(new AppUser("Intruso", "frequencia-intruso@example.com", "hash"));
        dashboard = dashboardRepository.save(new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, owner));
        discipline = newDiscipline("Cálculo", new BigDecimal("75.0"));
    }

    @ParameterizedTest
    @CsvSource({"0,100.00", "1,95.00", "5,75.00", "6,70.00", "20,0.00", "21,0.00"})
    void subtractsFivePointsPerAbsenceWithoutGoingBelowZero(int absences, String expected) {
        FrequencyResponse response = create(discipline, absences);

        assertEquals(new BigDecimal(expected), response.attendancePercentage());
        assertEquals(BigDecimal.valueOf(5), response.lossPerAbsence());
        assertEquals(5, response.maximumAbsences());
    }

    @Test
    void roundsTheAbsenceLimitDownToRespectTheMinimum() {
        Discipline target = newDiscipline("Física", new BigDecimal("76.0"));
        FrequencyResponse response = create(target, 4);
        assertEquals(new BigDecimal("80.00"), response.attendancePercentage());
        assertEquals(4, response.maximumAbsences());
    }

    @Test
    void allowsNoAbsenceWhenTheDisciplineRequiresFullAttendance() {
        Discipline strictDiscipline = newDiscipline("Laboratório", new BigDecimal("100.0"));

        FrequencyResponse response = create(strictDiscipline, 0);

        assertEquals(new BigDecimal("100.00"), response.attendancePercentage());
        assertEquals(0, response.maximumAbsences());
    }

    @Test
    void ignoresLegacyClassTotalsAndSharesAttendanceWithTheDisciplineScreen() {
        create(discipline, 6);
        entityManager.flush();
        entityManager.createNativeQuery("update frequency set total_classes = 60 where discipline_id = :id")
                .setParameter("id", discipline.getId()).executeUpdate();
        entityManager.clear();
        assertEquals(new BigDecimal("70.00"), frequencyService.findByDiscipline(
                owner.getId(), dashboard.getId(), discipline.getId()).attendancePercentage());
        assertEquals(new BigDecimal("70.00"), disciplineService.findById(
                owner.getId(), dashboard.getId(), discipline.getId()).attendancePercentage());
    }

    @Test
    void rejectsASecondFrequencyForTheSameDiscipline() {
        create(discipline, 6);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> create(discipline, 8)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void recalculatesAttendanceWhenAbsencesAreChangedOrReset() {
        create(discipline, 6);

        FrequencyResponse response = frequencyService.update(
                owner.getId(),
                dashboard.getId(),
                discipline.getId(),
                new UpdateFrequencyRequest(5)
        );

        assertEquals(new BigDecimal("75.00"), response.attendancePercentage());
        assertEquals(5, response.absences());
        assertEquals(5, response.maximumAbsences());
        FrequencyResponse reset = frequencyService.update(owner.getId(), dashboard.getId(),
                discipline.getId(), new UpdateFrequencyRequest(0));
        assertEquals(new BigDecimal("100.00"), reset.attendancePercentage());
    }

    @Test
    void readsBackTheFrequencyThatWasRecorded() {
        create(discipline, 6);

        FrequencyResponse response = frequencyService.findByDiscipline(
                owner.getId(),
                dashboard.getId(),
                discipline.getId()
        );

        assertEquals(6, response.absences());
        assertEquals(new BigDecimal("70.00"), response.attendancePercentage());
    }

    @Test
    void failsWhenTheFrequencyWasNeverRecorded() {
        assertNotFound(() -> frequencyService.findByDiscipline(
                owner.getId(),
                dashboard.getId(),
                discipline.getId()
        ));

        assertNotFound(() -> frequencyService.update(
                owner.getId(),
                dashboard.getId(),
                discipline.getId(),
                new UpdateFrequencyRequest(6)
        ));
    }

    @Test
    void hidesTheFrequencyOfAnotherUser() {
        create(discipline, 6);

        assertNotFound(() -> frequencyService.findByDiscipline(
                intruder.getId(),
                dashboard.getId(),
                discipline.getId()
        ));
    }

    @Test
    void failsWhenTheDisciplineDoesNotExist() {
        assertNotFound(() -> frequencyService.create(
                owner.getId(),
                dashboard.getId(),
                UUID.randomUUID(),
                new CreateFrequencyRequest(6)
        ));
    }

    private FrequencyResponse create(Discipline target, int absences) {
        return frequencyService.create(
                owner.getId(),
                dashboard.getId(),
                target.getId(),
                new CreateFrequencyRequest(absences)
        );
    }

    private Discipline newDiscipline(String name, BigDecimal minimumAttendancePercentage) {
        return disciplineRepository.save(new Discipline(
                name,
                "Professora Ana",
                "#4F46E5",
                new BigDecimal("6.00"),
                minimumAttendancePercentage,
                dashboard,
                List.of(),
                "2026.2",
                "2"
        ));
    }

    private void assertNotFound(Runnable access) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, access::run);

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }
}
