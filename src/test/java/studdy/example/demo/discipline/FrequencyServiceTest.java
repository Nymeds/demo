package studdy.example.demo.discipline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @Test
    void derivesTheAttendanceLimitsFromTheDisciplineMinimum() {
        FrequencyResponse response = create(discipline, 60, 6);

        assertEquals(new BigDecimal("90.00"), response.attendancePercentage());
        assertEquals(45, response.minimumAttendanceClasses());
        assertEquals(15, response.maximumAbsences());
    }

    @Test
    void roundsTheMinimumNumberOfClassesUpwards() {
        Discipline shortDiscipline = newDiscipline("Física", new BigDecimal("75.0"));

        // 75% de 50 é 37,5 aulas: arredonda para cima, senão o aluno passaria com menos presença.
        FrequencyResponse response = create(shortDiscipline, 50, 12);

        assertEquals(new BigDecimal("76.00"), response.attendancePercentage());
        assertEquals(38, response.minimumAttendanceClasses());
        assertEquals(12, response.maximumAbsences());
    }

    @Test
    void allowsNoAbsenceWhenTheDisciplineRequiresFullAttendance() {
        Discipline strictDiscipline = newDiscipline("Laboratório", new BigDecimal("100.0"));

        FrequencyResponse response = create(strictDiscipline, 60, 0);

        assertEquals(new BigDecimal("100.00"), response.attendancePercentage());
        assertEquals(60, response.minimumAttendanceClasses());
        assertEquals(0, response.maximumAbsences());
    }

    @Test
    void rejectsMoreAbsencesThanClasses() {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> create(discipline, 60, 61)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void rejectsASecondFrequencyForTheSameDiscipline() {
        create(discipline, 60, 6);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> create(discipline, 60, 8)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void recalculatesTheLimitsOnUpdate() {
        create(discipline, 60, 6);

        FrequencyResponse response = frequencyService.update(
                owner.getId(),
                dashboard.getId(),
                discipline.getId(),
                new UpdateFrequencyRequest(60, 15)
        );

        assertEquals(new BigDecimal("75.00"), response.attendancePercentage());
        assertEquals(15, response.absences());
        assertEquals(15, response.maximumAbsences());
    }

    @Test
    void readsBackTheFrequencyThatWasRecorded() {
        create(discipline, 60, 6);

        FrequencyResponse response = frequencyService.findByDiscipline(
                owner.getId(),
                dashboard.getId(),
                discipline.getId()
        );

        assertEquals(60, response.totalClasses());
        assertEquals(6, response.absences());
        assertEquals(new BigDecimal("90.00"), response.attendancePercentage());
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
                new UpdateFrequencyRequest(60, 6)
        ));
    }

    @Test
    void hidesTheFrequencyOfAnotherUser() {
        create(discipline, 60, 6);

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
                new CreateFrequencyRequest(60, 6)
        ));
    }

    private FrequencyResponse create(Discipline target, int totalClasses, int absences) {
        return frequencyService.create(
                owner.getId(),
                dashboard.getId(),
                target.getId(),
                new CreateFrequencyRequest(totalClasses, absences)
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
                List.of()
        ));
    }

    private void assertNotFound(Runnable access) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, access::run);

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }
}
