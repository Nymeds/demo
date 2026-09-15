package studdy.example.demo.settings;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.activities.Activity;
import studdy.example.demo.activities.ActivityRepository;
import studdy.example.demo.activities.ActivityStatus;
import studdy.example.demo.avatar.UserAvatar;
import studdy.example.demo.avatar.UserAvatarRepository;
import studdy.example.demo.calendar.CalendarEvent;
import studdy.example.demo.calendar.CalendarEventCategory;
import studdy.example.demo.calendar.CalendarEventRepository;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.discipline.ClassSchedule;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.discipline.Frequency;
import studdy.example.demo.discipline.FrequencyRepository;
import studdy.example.demo.grade.Grade;
import studdy.example.demo.grade.GradeRepository;
import studdy.example.demo.settings.dto.DeleteAccountRequest;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AccountDeletionServiceTest {

    private static final String PASSWORD = "senha-da-conta-1";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private FrequencyRepository frequencyRepository;

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private UserPreferencesRepository preferencesRepository;

    @Autowired
    private UserAvatarRepository avatarRepository;

    @Autowired
    private AccountDeletionService accountDeletionService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void deletesTheAccountAndEverythingItOwns() {
        AppUser owner = newUser("excluir@example.com");
        OwnedData data = createAcademicData(owner);
        UUID preferencesId = preferencesRepository.save(new UserPreferences(owner)).getId();
        UUID avatarId = avatarRepository.save(new UserAvatar(owner, new byte[] {1, 2, 3}, "image/jpeg")).getId();

        deleteInANewRequest(owner.getId());

        assertFalse(userRepository.existsById(owner.getId()));
        assertFalse(avatarRepository.existsById(avatarId), "a foto de perfil ficou órfã");
        assertFalse(preferencesRepository.existsById(preferencesId), "as preferências ficaram órfãs");
        assertFalse(dashboardRepository.existsById(data.dashboardId()), "o dashboard ficou órfão");
        assertFalse(disciplineRepository.existsById(data.disciplineId()), "a disciplina ficou órfã");
        assertFalse(gradeRepository.existsById(data.gradeId()), "a nota ficou órfã");
        assertFalse(activityRepository.existsById(data.activityId()), "a atividade ficou órfã");
        assertFalse(frequencyRepository.existsById(data.frequencyId()), "a frequência ficou órfã");
        assertFalse(calendarEventRepository.existsById(data.eventId()), "o evento ficou órfão");
    }

    @Test
    void keepsTheDataOfOtherAccounts() {
        AppUser owner = newUser("excluir@example.com");
        AppUser otherUser = newUser("continua@example.com");
        createAcademicData(owner);
        OwnedData kept = createAcademicData(otherUser);

        deleteInANewRequest(owner.getId());

        assertTrue(userRepository.existsById(otherUser.getId()));
        assertTrue(dashboardRepository.existsById(kept.dashboardId()));
        assertTrue(disciplineRepository.existsById(kept.disciplineId()));
        assertTrue(gradeRepository.existsById(kept.gradeId()));
        assertTrue(activityRepository.existsById(kept.activityId()));
        assertTrue(frequencyRepository.existsById(kept.frequencyId()));
        assertTrue(calendarEventRepository.existsById(kept.eventId()));
    }

    @Test
    void deletesAnAccountThatNeverCreatedADashboard() {
        AppUser owner = newUser("sem-dashboard@example.com");

        deleteInANewRequest(owner.getId());

        assertFalse(userRepository.existsById(owner.getId()));
    }

    @Test
    void keepsTheAccountWhenThePasswordIsWrong() {
        AppUser owner = newUser("senha-errada@example.com");
        OwnedData data = createAcademicData(owner);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> accountDeletionService.delete(owner.getId(), new DeleteAccountRequest("senha-errada-000"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(userRepository.existsById(owner.getId()));
        assertTrue(dashboardRepository.existsById(data.dashboardId()));
    }

    // Os filhos foram salvos pelos próprios repositórios e a disciplina em memória não sabe deles.
    // Limpar o contexto reproduz a exclusão chegando numa requisição separada, como na vida real.
    private void deleteInANewRequest(UUID userId) {
        entityManager.flush();
        entityManager.clear();

        accountDeletionService.delete(userId, new DeleteAccountRequest(PASSWORD));
        entityManager.flush();
    }

    private AppUser newUser(String email) {
        return userRepository.save(new AppUser("Estudante", email, passwordEncoder.encode(PASSWORD)));
    }

    private OwnedData createAcademicData(AppUser owner) {
        Dashboard dashboard = dashboardRepository.save(new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, owner));
        Discipline discipline = disciplineRepository.save(new Discipline(
                "Cálculo",
                "Professora Ana",
                "#4F46E5",
                new BigDecimal("6.00"),
                new BigDecimal("75.00"),
                dashboard,
                List.of(new ClassSchedule(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))),
                "2026.2",
                "2"
        ));

        UUID gradeId = gradeRepository.save(
                new Grade(discipline, "Prova 1", new BigDecimal("8.00"), LocalDate.of(2026, 8, 15))
        ).getId();
        UUID activityId = activityRepository.save(new Activity(
                "Trabalho final",
                "Entrega em dupla",
                LocalDate.of(2026, 9, 1),
                ActivityStatus.PENDING,
                discipline
        )).getId();
        UUID frequencyId = frequencyRepository.save(new Frequency(discipline, 60, 6)).getId();
        UUID eventId = calendarEventRepository.save(new CalendarEvent(
                dashboard,
                discipline,
                "Prova de Cálculo",
                null,
                CalendarEventCategory.EXAM,
                LocalDateTime.of(2026, 9, 20, 8, 0),
                null
        )).getId();

        return new OwnedData(dashboard.getId(), discipline.getId(), gradeId, activityId, frequencyId, eventId);
    }

    private record OwnedData(
            UUID dashboardId,
            UUID disciplineId,
            UUID gradeId,
            UUID activityId,
            UUID frequencyId,
            UUID eventId
    ) {
    }
}
