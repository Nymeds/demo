package studdy.example.demo.simulator;

import jakarta.validation.Valid;
import studdy.example.demo.simulator.dto.SimulatorRequest;
import studdy.example.demo.simulator.dto.SimulatorResponse;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboards/{dashboardId}/disciplines/{disciplineId}/simulator")
public class SimulatorController {

    private final SimulatorService simulatorService;

    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

/*
 * O POST retorna 200 OK porque este endpoint apenas realiza
 * um cálculo de simulação e não cria um novo recurso.
 */
    
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