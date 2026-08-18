package studdy.example.demo.simulatornotes;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import studdy.example.demo.simulatornotes.dto.SimulatorRequest;
import studdy.example.demo.simulatornotes.dto.SimulatorResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboards/{dashboardId}/disciplines/{disciplineId}/simulator")
public class SimulatorController {

    private final SimulatorService simulatorService;

    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @PostMapping
    public SimulatorResponse simulate(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId,
            @Valid @RequestBody SimulatorRequest request
    ) {
        return simulatorService.simulate(
                userId,
                dashboardId,
                disciplineId,
                request
        );
    }
}