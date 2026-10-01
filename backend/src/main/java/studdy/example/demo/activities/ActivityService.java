package studdy.example.demo.activities;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import studdy.example.demo.activities.dto.ActivityResponse;
import studdy.example.demo.activities.dto.CreateActivityRequest;
import studdy.example.demo.activities.dto.UpdateActivityRequest;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineAccessService;
import studdy.example.demo.grade.Grade;
import studdy.example.demo.grade.GradeRepository;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class ActivityService {

    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ActivityRepository activityRepository;
    private final DisciplineAccessService disciplineAccessService;
    private final GradeRepository gradeRepository;

    public ActivityService(
            ActivityRepository activityRepository,
            DisciplineAccessService disciplineAccessService,
            GradeRepository gradeRepository
    ) {
        this.activityRepository = activityRepository;
        this.disciplineAccessService = disciplineAccessService;
        this.gradeRepository = gradeRepository;
    }

    @Transactional
    public ActivityResponse create(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            CreateActivityRequest request
    ) {

        Discipline discipline = disciplineAccessService.findOwnedDiscipline(
                userId,
                dashboardId,
                disciplineId
        );

        Activity activity = new Activity(
                request.title().trim(),
                normalizeDescription(request.description()),
                request.dueDate(),
                request.status(),
                request.type(),
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
            UUID disciplineId,
            ActivityType type
    ) {

        disciplineAccessService.findOwnedDiscipline(
                userId,
                dashboardId,
                disciplineId
        );

        List<Activity> activities = type != null
                ? activityRepository.findAllByDiscipline_IdAndTypeOrderByDueDateAsc(disciplineId, type)
                : activityRepository.findAllByDiscipline_IdOrderByDueDateAsc(disciplineId);

        return activities
                .stream()
                .map(ActivityResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> findAllByDashboard(
            UUID userId,
            UUID dashboardId,
            ActivityType type
    ) {

        disciplineAccessService.findOwnedDashboard(
                userId,
                dashboardId
        );

        List<Activity> activities = type != null
                ? activityRepository.findAllByDiscipline_Dashboard_IdAndTypeOrderByDueDateAscIdAsc(dashboardId, type)
                : activityRepository.findAllByDiscipline_Dashboard_IdOrderByDueDateAscIdAsc(dashboardId);

        return activities
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

        // Ordem de locks: atividade primeiro, nota depois (mesma ordem do GradeService).
        disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId);
        Activity activity = activityRepository
                .findByIdAndDiscipline_IdForUpdate(activityId, disciplineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Atividade não encontrada."
                ));

        gradeRepository.findByActivity_IdForUpdate(activityId).ifPresent(grade -> {
            if (request.dueDate().isAfter(grade.getRecordedAt())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "O prazo não pode ficar depois de "
                                + grade.getRecordedAt().format(BRAZILIAN_DATE)
                                + ", data da nota lançada. Exclua a nota ou ajuste a data dela (até hoje) antes."
                );
            }
        });

        activity.update(
                request.title().trim(),
                normalizeDescription(request.description()),
                request.dueDate(),
                request.status(),
                request.type()
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

    private Activity findOwnedActivity(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            UUID activityId
    ) {

        disciplineAccessService.findOwnedDiscipline(
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