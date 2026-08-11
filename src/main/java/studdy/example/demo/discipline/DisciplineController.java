package studdy.example.demo.discipline;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import studdy.example.demo.discipline.dto.CreateDisciplineRequest;
import studdy.example.demo.discipline.dto.DisciplineResponse;
import studdy.example.demo.discipline.dto.UpdateDisciplineRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboards/{dashboardId}/disciplines")
public class DisciplineController {

    private final DisciplineService disciplineService;

    public DisciplineController(DisciplineService disciplineService) {
        this.disciplineService = disciplineService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DisciplineResponse create(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @Valid @RequestBody CreateDisciplineRequest request
    ) {
        return disciplineService.create(userId, dashboardId, request);
    }

    @GetMapping
    public List<DisciplineResponse> findAll(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId
    ) {
        return disciplineService.findAll(userId, dashboardId);
    }

    @GetMapping("/{disciplineId}")
    public DisciplineResponse findById(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId
    ) {
        return disciplineService.findById(userId, dashboardId, disciplineId);
    }

    @PutMapping("/{disciplineId}")
    public DisciplineResponse update(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId,
            @Valid @RequestBody UpdateDisciplineRequest request
    ) {
        return disciplineService.update(userId, dashboardId, disciplineId, request);
    }

    @DeleteMapping("/{disciplineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId
    ) {
        disciplineService.delete(userId, dashboardId, disciplineId);
    }
}
