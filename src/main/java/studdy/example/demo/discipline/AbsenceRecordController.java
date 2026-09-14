package studdy.example.demo.discipline;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import studdy.example.demo.discipline.dto.AbsenceRecordResponse;
import studdy.example.demo.discipline.dto.CreateAbsenceRecordRequest;

@RestController
@RequestMapping("/api/v1/dashboards/{dashboardId}/disciplines/{disciplineId}/frequency/absences")
public class AbsenceRecordController {

    private final AbsenceRecordService absenceRecordService;

    public AbsenceRecordController(AbsenceRecordService absenceRecordService) {
        this.absenceRecordService = absenceRecordService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AbsenceRecordResponse create(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId,
            @Valid @RequestBody CreateAbsenceRecordRequest request
    ) {
        return absenceRecordService.create(userId, dashboardId, disciplineId, request);
    }

    @GetMapping
    public List<AbsenceRecordResponse> findAll(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId
    ) {
        return absenceRecordService.findAll(userId, dashboardId, disciplineId);
    }

    @DeleteMapping("/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId,
            @PathVariable UUID recordId
    ) {
        absenceRecordService.delete(userId, dashboardId, disciplineId, recordId);
    }
}
