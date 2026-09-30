package studdy.example.demo.dashboard.dto;

import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardStatus;

import java.util.UUID;

public record DashboardResponse(
        UUID id,
        String name,
        DashboardStatus status
) {
    public static DashboardResponse from(Dashboard dashboard) {
        return new DashboardResponse(
                dashboard.getId(),
                dashboard.getName(),
                dashboard.getStatus()
        );
    }
}
