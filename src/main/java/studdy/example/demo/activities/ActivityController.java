package studdy.example.demo.activities;

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

import studdy.example.demo.activities.dto.ActivityResponse;
import studdy.example.demo.activities.dto.CreateActivityRequest;
import studdy.example.demo.activities.dto.UpdateActivityRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboards/{dashboardId}/disciplines/{disciplineId}/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(
            ActivityService activityService
    ) {
        this.activityService = activityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityResponse create(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId,
            @Valid @RequestBody CreateActivityRequest request
    ) {

        return activityService.create(
                userId,
                dashboardId,
                disciplineId,
                request
        );
    }

    @GetMapping
    public List<ActivityResponse> findAll(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId
    ) {

        return activityService.findAll(
                userId,
                dashboardId,
                disciplineId
        );
    }

    @GetMapping("/{activityId}")
    public ActivityResponse findById(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId,
            @PathVariable UUID activityId
    ) {

        return activityService.findById(
                userId,
                dashboardId,
                disciplineId,
                activityId
        );
    }

    @PutMapping("/{activityId}")
    public ActivityResponse update(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId,
            @PathVariable UUID activityId,
            @Valid @RequestBody UpdateActivityRequest request
    ) {

        return activityService.update(
                userId,
                dashboardId,
                disciplineId,
                activityId,
                request
        );
    }

    @DeleteMapping("/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID disciplineId,
            @PathVariable UUID activityId
    ) {

        activityService.delete(
                userId,
                dashboardId,
                disciplineId,
                activityId
        );
    }
}