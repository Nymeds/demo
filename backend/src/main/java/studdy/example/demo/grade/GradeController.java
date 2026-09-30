package studdy.example.demo.grade;

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
import studdy.example.demo.grade.dto.CreateGradeRequest;
import studdy.example.demo.grade.dto.GradeResponse;
import studdy.example.demo.grade.dto.GradeSummaryResponse;
import studdy.example.demo.grade.dto.UpdateGradeRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboards/{dashboardId}/disciplines/{disciplineId}/grades")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GradeResponse create(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId,
            @Valid @RequestBody CreateGradeRequest request
    ) {
        return gradeService.create(userId, dashboardId, disciplineId, request);
    }

    @GetMapping
    public List<GradeResponse> findAll(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId
    ) {
        return gradeService.findAll(userId, dashboardId, disciplineId);
    }

    @GetMapping("/summary")
    public GradeSummaryResponse summary(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId
    ) {
        return gradeService.summary(userId, dashboardId, disciplineId);
    }

    @GetMapping("/{gradeId}")
    public GradeResponse findById(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId,
            @PathVariable UUID gradeId
    ) {
        return gradeService.findById(userId, dashboardId, disciplineId, gradeId);
    }

    @PutMapping("/{gradeId}")
    public GradeResponse update(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId,
            @PathVariable UUID gradeId,
            @Valid @RequestBody UpdateGradeRequest request
    ) {
        return gradeService.update(userId, dashboardId, disciplineId, gradeId, request);
    }

    @DeleteMapping("/{gradeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId,
            @PathVariable UUID gradeId
    ) {
        gradeService.delete(userId, dashboardId, disciplineId, gradeId);
    }
}
