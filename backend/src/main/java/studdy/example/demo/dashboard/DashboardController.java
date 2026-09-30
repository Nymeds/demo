package studdy.example.demo.dashboard;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import studdy.example.demo.dashboard.dto.CreateDashboardRequest;
import studdy.example.demo.dashboard.dto.DashboardResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboards")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DashboardResponse create(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateDashboardRequest request
    ) {
        return dashboardService.create(userId, request);
    }

    @GetMapping
    public List<DashboardResponse> findAll(@AuthenticationPrincipal UUID userId) {
        return dashboardService.findAll(userId);
    }
}
