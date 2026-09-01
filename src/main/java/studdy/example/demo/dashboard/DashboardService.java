package studdy.example.demo.dashboard;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.dashboard.dto.CreateDashboardRequest;
import studdy.example.demo.dashboard.dto.DashboardResponse;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final UserRepository userRepository;

    public DashboardService(DashboardRepository dashboardRepository, UserRepository userRepository) {
        this.dashboardRepository = dashboardRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DashboardResponse create(UUID userId, CreateDashboardRequest request) {
        AppUser owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        Dashboard dashboard = new Dashboard(
                request.name().trim(),
                request.status(),
                owner
        );

        return DashboardResponse.from(dashboardRepository.save(dashboard));
    }

    @Transactional(readOnly = true)
    public List<DashboardResponse> findAll(UUID userId) {
        return dashboardRepository.findAllByOwner_IdOrderByNameAsc(userId).stream()
                .map(DashboardResponse::from)
                .toList();
    }
}
