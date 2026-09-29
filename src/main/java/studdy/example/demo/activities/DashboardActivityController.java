package studdy.example.demo.activities;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import studdy.example.demo.activities.dto.ActivityResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboards/{dashboardId}/activities")
public class DashboardActivityController {

    private final ActivityService activityService;

    public DashboardActivityController(
            ActivityService activityService
    ) {
        this.activityService = activityService;
    }

    @GetMapping
    public List<ActivityResponse> findAll(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @RequestParam(required = false) ActivityType type
    ) {

        return activityService.findAllByDashboard(
                userId,
                dashboardId,
                type
        );
    }
}
