package studdy.example.demo.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.avatar.UserAvatarRepository;
import studdy.example.demo.calendar.CalendarEventRepository;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.settings.dto.DeleteAccountRequest;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
public class AccountDeletionService {

    private final AccountCredentials accountCredentials;
    private final UserRepository userRepository;
    private final DashboardRepository dashboardRepository;
    private final DisciplineRepository disciplineRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final UserAvatarRepository avatarRepository;

    public AccountDeletionService(
            AccountCredentials accountCredentials,
            UserRepository userRepository,
            DashboardRepository dashboardRepository,
            DisciplineRepository disciplineRepository,
            CalendarEventRepository calendarEventRepository,
            UserPreferencesRepository preferencesRepository,
            UserAvatarRepository avatarRepository
    ) {
        this.accountCredentials = accountCredentials;
        this.userRepository = userRepository;
        this.dashboardRepository = dashboardRepository;
        this.disciplineRepository = disciplineRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.preferencesRepository = preferencesRepository;
        this.avatarRepository = avatarRepository;
    }

    // A ordem segue as chaves estrangeiras, dos filhos para o pai. Notas, atividades, frequência e
    // horários saem junto com cada disciplina pelo cascade já mapeado em Discipline.
    @Transactional
    public void delete(UUID userId, DeleteAccountRequest request) {
        AppUser user = accountCredentials.findUser(userId);
        accountCredentials.requireCurrentPassword(user, request.currentPassword());

        List<Dashboard> dashboards = dashboardRepository.findAllByOwner_IdOrderByNameAsc(userId);
        List<UUID> dashboardIds = dashboards.stream().map(Dashboard::getId).toList();

        if (!dashboardIds.isEmpty()) {
            calendarEventRepository.deleteAllByDashboard_IdIn(dashboardIds);
            dashboardIds.forEach(dashboardId -> disciplineRepository.deleteAll(
                    disciplineRepository.findAllByDashboard_IdOrderByNameAsc(dashboardId)
            ));
            dashboardRepository.deleteAll(dashboards);
        }

        avatarRepository.deleteByUser_Id(userId);
        preferencesRepository.deleteByUser_Id(userId);
        userRepository.delete(user);
    }
}
