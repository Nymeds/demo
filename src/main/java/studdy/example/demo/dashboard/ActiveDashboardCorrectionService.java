package studdy.example.demo.dashboard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Corrects legacy data: keeps a single ACTIVE dashboard per owner. Idempotent; never deletes data.
 * Dashboard has no timestamp fields, so the survivor is the one with the most disciplines (ties: highest id).
 */
@Service
public class ActiveDashboardCorrectionService {

    private final DashboardRepository dashboardRepository;

    public ActiveDashboardCorrectionService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @Transactional
    public Result correct() {
        List<UUID> ownerIds = dashboardRepository
                .findOwnerIdsWithMoreThanOneDashboardInStatus(DashboardStatus.ACTIVE);
        int deactivated = 0;

        for (UUID ownerId : ownerIds) {
            List<Dashboard> active = dashboardRepository
                    .findAllByOwner_IdAndStatus(ownerId, DashboardStatus.ACTIVE);
            Map<UUID, Long> counts = new HashMap<>();
            dashboardRepository.countDisciplinesByOwnerAndStatus(ownerId, DashboardStatus.ACTIVE)
                    .forEach(c -> counts.put(c.getDashboardId(), c.getDisciplineCount()));
            Dashboard keep = active.stream()
                    .max(Comparator.<Dashboard, Long>comparing(dash -> counts.getOrDefault(dash.getId(), 0L))
                            .thenComparing(Dashboard::getId))
                    .orElseThrow();
            List<Dashboard> toDeactivate = active.stream().filter(dash -> dash != keep).toList();
            toDeactivate.forEach(Dashboard::deactivate);
            dashboardRepository.saveAll(toDeactivate);
            deactivated += toDeactivate.size();
        }
        return new Result(ownerIds.size(), deactivated);
    }

    public record Result(int accountsAdjusted, int dashboardsDeactivated) {
    }
}
