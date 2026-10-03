package studdy.example.demo.discipline;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;

import java.util.UUID;

@Service
public class DisciplineAccessService {

    private final DisciplineRepository disciplineRepository;
    private final DashboardRepository dashboardRepository;

    public DisciplineAccessService(
            DisciplineRepository disciplineRepository,
            DashboardRepository dashboardRepository
    ) {
        this.disciplineRepository = disciplineRepository;
        this.dashboardRepository = dashboardRepository;
    }

    @Transactional(readOnly = true)
    public Dashboard findOwnedDashboard(UUID userId, UUID dashboardId) {
        return dashboardRepository.findByIdAndOwner_Id(dashboardId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Dashboard não encontrado."
                ));
    }

    @Transactional(readOnly = true)
    public Discipline findOwnedDiscipline(UUID userId, UUID dashboardId, UUID disciplineId) {
        findOwnedDashboard(userId, dashboardId);

        return disciplineRepository.findByIdAndDashboard_Id(disciplineId, dashboardId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Disciplina não encontrada."
                ));
    }
}
