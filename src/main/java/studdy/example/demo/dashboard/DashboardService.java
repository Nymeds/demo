package studdy.example.demo.dashboard;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.dashboard.dto.CreateDashboardRequest;
import studdy.example.demo.dashboard.dto.DashboardResponse;
import studdy.example.demo.user.AppUser;

import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @Transactional
    public DashboardResponse create(UUID userId, CreateDashboardRequest request) {
        AppUser owner = dashboardRepository.findOwnerForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        Dashboard dashboard = new Dashboard(
                request.name().trim(),
                request.status(),
                owner
        );

        Dashboard saved = dashboardRepository.save(dashboard);

        if (saved.getStatus() == DashboardStatus.ACTIVE) {
            deactivateOtherActiveDashboards(userId, saved.getId());
        }

        return DashboardResponse.from(saved);
    }

    private void deactivateOtherActiveDashboards(UUID ownerId, UUID keepDashboardId) {
        List<Dashboard> otherActiveDashboards = dashboardRepository
                .findAllByOwner_IdAndStatusAndIdNot(ownerId, DashboardStatus.ACTIVE, keepDashboardId);

        otherActiveDashboards.forEach(Dashboard::deactivate);

        dashboardRepository.saveAll(otherActiveDashboards);
    }

    @Transactional(readOnly = true)
    public List<DashboardResponse> findAll(UUID userId) {
        return dashboardRepository.findAllByOwner_IdOrderByNameAsc(userId).stream()
                .map(DashboardResponse::from)
                .toList();
    }
}
