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

        // Desativa os outros antes do save: o índice único parcial ux_dashboards_one_active_per_owner
        // rejeita dois ACTIVE do mesmo dono, e o auto-flush da consulta inseriria o novo primeiro.
        if (dashboard.getStatus() == DashboardStatus.ACTIVE) {
            deactivateOtherActiveDashboards(userId);
        }

        Dashboard saved = dashboardRepository.save(dashboard);

        return DashboardResponse.from(saved);
    }

    private void deactivateOtherActiveDashboards(UUID ownerId) {
        List<Dashboard> otherActiveDashboards = dashboardRepository
                .findAllByOwner_IdAndStatus(ownerId, DashboardStatus.ACTIVE);

        otherActiveDashboards.forEach(Dashboard::deactivate);

        // Flush explícito: o Hibernate executa INSERTs antes de UPDATEs, então sem ele o novo ACTIVE
        // entraria antes de os antigos virarem INACTIVE.
        dashboardRepository.saveAllAndFlush(otherActiveDashboards);
    }

    @Transactional(readOnly = true)
    public List<DashboardResponse> findAll(UUID userId) {
        return dashboardRepository.findAllByOwner_IdOrderByNameAsc(userId).stream()
                .map(DashboardResponse::from)
                .toList();
    }
}
