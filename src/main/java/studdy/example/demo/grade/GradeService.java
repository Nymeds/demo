package studdy.example.demo.grade;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.activities.Activity;
import studdy.example.demo.activities.ActivityRepository;
import studdy.example.demo.discipline.AcademicPerformanceService;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineAccessService;
import studdy.example.demo.grade.dto.CreateGradeRequest;
import studdy.example.demo.grade.dto.GradeResponse;
import studdy.example.demo.grade.dto.GradeSummaryResponse;
import studdy.example.demo.grade.dto.UpdateGradeRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class GradeService {

    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final GradeRepository gradeRepository;
    private final ActivityRepository activityRepository;
    private final DisciplineAccessService disciplineAccessService;
    private final AcademicPerformanceService academicPerformanceService;

    public GradeService(
            GradeRepository gradeRepository,
            ActivityRepository activityRepository,
            DisciplineAccessService disciplineAccessService,
            AcademicPerformanceService academicPerformanceService
    ) {
        this.gradeRepository = gradeRepository;
        this.activityRepository = activityRepository;
        this.disciplineAccessService = disciplineAccessService;
        this.academicPerformanceService = academicPerformanceService;
    }

    @Transactional
    public GradeResponse create(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            CreateGradeRequest request
    ) {
        Discipline discipline = findOwnedDiscipline(userId, dashboardId, disciplineId);
        Activity activity = findLinkableActivity(disciplineId, request.activityId(), request.recordedAt(), null);

        Grade grade = new Grade(
                discipline,
                request.assessmentName().trim(),
                request.score(),
                request.recordedAt(),
                activity
        );

        try {
            return GradeResponse.from(gradeRepository.saveAndFlush(grade));
        } catch (DataIntegrityViolationException exception) {
            throw activityAlreadyGraded();
        }
    }

    @Transactional(readOnly = true)
    public List<GradeResponse> findAll(UUID userId, UUID dashboardId, UUID disciplineId) {
        findOwnedDiscipline(userId, dashboardId, disciplineId);

        return gradeRepository.findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(disciplineId).stream()
                .map(GradeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GradeSummaryResponse summary(UUID userId, UUID dashboardId, UUID disciplineId) {
        Discipline discipline = findOwnedDiscipline(userId, dashboardId, disciplineId);

        List<Grade> grades = gradeRepository.findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(disciplineId);
        return GradeSummaryResponse.from(
                disciplineId,
                grades.size(),
                academicPerformanceService.calculate(grades, discipline.getPassingAverage())
        );
    }

    @Transactional(readOnly = true)
    public GradeResponse findById(UUID userId, UUID dashboardId, UUID disciplineId, UUID gradeId) {
        findOwnedDiscipline(userId, dashboardId, disciplineId);
        return GradeResponse.from(findGrade(disciplineId, gradeId));
    }

    @Transactional
    public GradeResponse update(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            UUID gradeId,
            UpdateGradeRequest request
    ) {
        findOwnedDiscipline(userId, dashboardId, disciplineId);
        Grade grade = findGrade(disciplineId, gradeId);
        Activity activity = findLinkableActivity(disciplineId, request.activityId(), request.recordedAt(), gradeId);

        grade.update(
                request.assessmentName().trim(),
                request.score(),
                request.recordedAt(),
                activity
        );

        try {
            gradeRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw activityAlreadyGraded();
        }

        return GradeResponse.from(grade);
    }

    @Transactional
    public void delete(UUID userId, UUID dashboardId, UUID disciplineId, UUID gradeId) {
        findOwnedDiscipline(userId, dashboardId, disciplineId);
        gradeRepository.delete(findGrade(disciplineId, gradeId));
    }

    private Discipline findOwnedDiscipline(UUID userId, UUID dashboardId, UUID disciplineId) {
        return disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId);
    }

    private Grade findGrade(UUID disciplineId, UUID gradeId) {
        return gradeRepository.findByIdAndDiscipline_Id(gradeId, disciplineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Nota não encontrada."
                ));
    }

    // A atividade é o que comprova a nota (relatório: "uma nota deverá estar relacionada a uma
    // avaliação"). Por isso ela precisa ser da mesma disciplina, já ter acontecido e não ter outra nota.
    private Activity findLinkableActivity(UUID disciplineId, UUID activityId, LocalDate recordedAt, UUID currentGradeId) {
        if (activityId == null) {
            return null;
        }

        Activity activity = activityRepository.findByIdAndDiscipline_Id(activityId, disciplineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Avaliação não encontrada nesta disciplina."
                ));

        if (activity.getDueDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Essa avaliação ainda não aconteceu. Lance a nota a partir de " + activity.getDueDate().format(BRAZILIAN_DATE) + "."
            );
        }

        if (recordedAt.isBefore(activity.getDueDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A nota não pode ter data anterior à avaliação (" + activity.getDueDate().format(BRAZILIAN_DATE) + ")."
            );
        }

        boolean alreadyGraded = currentGradeId == null
                ? gradeRepository.existsByActivity_Id(activityId)
                : gradeRepository.existsByActivity_IdAndIdNot(activityId, currentGradeId);

        if (alreadyGraded) {
            throw activityAlreadyGraded();
        }

        return activity;
    }

    // A checagem acima cobre o uso normal; a restrição única do banco cobre dois envios simultâneos.
    // Depois de uma falha no flush a sessão do Hibernate fica inutilizável: quem captura a exceção
    // só pode lançar este 409, sem nenhuma outra operação no banco no mesmo método.
    private ResponseStatusException activityAlreadyGraded() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Essa avaliação já tem uma nota lançada."
        );
    }
}
