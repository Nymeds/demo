package studdy.example.demo.gradebook;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import studdy.example.demo.gradebook.dto.GradebookEntryResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboards/{dashboardId}/gradebook")
public class GradebookController {

    private final GradebookService gradebookService;

    public GradebookController(GradebookService gradebookService) {
        this.gradebookService = gradebookService;
    }

    @GetMapping
    public List<GradebookEntryResponse> findAll(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId
    ) {
        return gradebookService.findAll(userId, dashboardId);
    }
}
