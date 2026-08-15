package studdy.example.demo.discipline;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import studdy.example.demo.discipline.dto.CreateFrequencyRequest;
import studdy.example.demo.discipline.dto.FrequencyResponse;
import studdy.example.demo.discipline.dto.UpdateFrequencyRequest;

@RestController
@RequestMapping("/api/v1/dashboards/{dashboardId}/disciplines/{disciplineId}/frequency")
public class FrequencyController {

    private final FrequencyService frequencyService;

    public FrequencyController(FrequencyService frequencyService) {
        this.frequencyService = frequencyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FrequencyResponse create(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId,
            @Valid @RequestBody CreateFrequencyRequest request
    ) {
        return frequencyService.create(
                userId,
                dashboardId,
                disciplineId,
                request
        );
    }
    @PutMapping
public FrequencyResponse update(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID dashboardId,
        @PathVariable UUID disciplineId,
        @Valid @RequestBody UpdateFrequencyRequest request
) {
    return frequencyService.update(
            userId,
            dashboardId,
            disciplineId,
            request
    );
}
@GetMapping
public FrequencyResponse findByDiscipline(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID dashboardId,
        @PathVariable UUID disciplineId
) {
    return frequencyService.findByDiscipline(
            userId,
            dashboardId,
            disciplineId
    );
}
}