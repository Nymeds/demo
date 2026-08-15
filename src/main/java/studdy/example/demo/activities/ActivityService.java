package studdy.example.demo.activities;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import studdy.example.demo.activities.dto.ActivityResponse;
import studdy.example.demo.activities.dto.CreateActivityRequest;
import studdy.example.demo.activities.dto.UpdateActivityRequest;

import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;

import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final DisciplineRepository disciplineRepository;
    private final DashboardRepository dashboardRepository;

    public ActivityService(
            ActivityRepository activityRepository,
            DisciplineRepository disciplineRepository,
            DashboardRepository dashboardRepository
    ) {
        this.activityRepository = activityRepository;
        this.disciplineRepository = disciplineRepository;
        this.dashboardRepository = dashboardRepository;
    }

    @Transactional
    public ActivityResponse create(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            CreateActivityRequest request
    ) {

        Discipline discipline = findOwnedDiscipline(
                userId,
                dashboardId,
                disciplineId
        );

        Activity activity = new Activity(
                request.title().trim(),
                normalizeDescription(request.description()),
                request.dueDate(),
                request.status(),
                discipline
        );

        return ActivityResponse.from(
                activityRepository.save(activity)
        );
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> findAll(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId
    ) {

        findOwnedDiscipline(
                userId,
                dashboardId,
                disciplineId
        );

        return activityRepository
                .findAllByDiscipline_IdOrderByDueDateAsc(disciplineId)
                .stream()
                .map(ActivityResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ActivityResponse findById(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            UUID activityId
    ) {

        Activity activity = findOwnedActivity(
                userId,
                dashboardId,
                disciplineId,
                activityId
        );

        return ActivityResponse.from(activity);
    }

    @Transactional
    public ActivityResponse update(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            UUID activityId,
            UpdateActivityRequest request
    ) {

        Activity activity = findOwnedActivity(
                userId,
                dashboardId,
                disciplineId,
                activityId
        );

        activity.update(
                request.title().trim(),
                normalizeDescription(request.description()),
                request.dueDate(),
                request.status()
        );

        return ActivityResponse.from(activity);
    }

    @Transactional
    public void delete(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            UUID activityId
    ) {

        Activity activity = findOwnedActivity(
                userId,
                dashboardId,
                disciplineId,
                activityId
        );

        activityRepository.delete(activity);
    }

    private Dashboard findOwnedDashboard(
            UUID userId,
            UUID dashboardId
    ) {

        return dashboardRepository
                .findByIdAndOwner_Id(dashboardId, userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Dashboard não encontrado."
                        )
                );
    }

    private Discipline findOwnedDiscipline(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId
    ) {

        findOwnedDashboard(userId, dashboardId);

        return disciplineRepository
                .findByIdAndDashboard_Id(disciplineId, dashboardId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Disciplina não encontrada."
                        )
                );
    }

    private Activity findOwnedActivity(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            UUID activityId
    ) {

        findOwnedDiscipline(
                userId,
                dashboardId,
                disciplineId
        );

        return activityRepository
                .findByIdAndDiscipline_Id(activityId, disciplineId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Atividade não encontrada."
                        )
                );
    }

    private String normalizeDescription(String description) {

        if (description == null) {
            return null;
        }

        String normalized = description.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}
