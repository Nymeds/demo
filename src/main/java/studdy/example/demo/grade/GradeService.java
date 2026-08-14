package studdy.example.demo.grade;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.grade.dto.CreateGradeRequest;
import studdy.example.demo.grade.dto.GradeResponse;
import studdy.example.demo.grade.dto.GradeSummaryResponse;
import studdy.example.demo.grade.dto.UpdateGradeRequest;

import java.util.List;
import java.util.UUID;

@Service
public class GradeService {

    private final GradeRepository gradeRepository;
    private final DisciplineRepository disciplineRepository;
    private final DashboardRepository dashboardRepository;

    public GradeService(
            GradeRepository gradeRepository,
            DisciplineRepository disciplineRepository,
            DashboardRepository dashboardRepository
    ) {
        this.gradeRepository = gradeRepository;
        this.disciplineRepository = disciplineRepository;
        this.dashboardRepository = dashboardRepository;
    }

    @Transactional
    public GradeResponse create(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            CreateGradeRequest request
    ) {
        Discipline discipline = findOwnedDiscipline(userId, dashboardId, disciplineId);

        Grade grade = new Grade(
                discipline,
                request.assessmentName().trim(),
                request.score(),
                request.weight(),
                request.recordedAt()
        );

        return GradeResponse.from(gradeRepository.save(grade));
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
        findOwnedDiscipline(userId, dashboardId, disciplineId);

        List<Grade> grades = gradeRepository.findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(disciplineId);
        return GradeSummaryResponse.from(disciplineId, grades);
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

        grade.update(
                request.assessmentName().trim(),
                request.score(),
                request.weight(),
                request.recordedAt()
        );

        return GradeResponse.from(grade);
    }

    @Transactional
    public void delete(UUID userId, UUID dashboardId, UUID disciplineId, UUID gradeId) {
        findOwnedDiscipline(userId, dashboardId, disciplineId);
        gradeRepository.delete(findGrade(disciplineId, gradeId));
    }

    private Discipline findOwnedDiscipline(UUID userId, UUID dashboardId, UUID disciplineId) {
        if (dashboardRepository.findByIdAndOwner_Id(dashboardId, userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dashboard não encontrado.");
        }

        return disciplineRepository.findByIdAndDashboard_Id(disciplineId, dashboardId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Disciplina não encontrada."
                ));
    }

    private Grade findGrade(UUID disciplineId, UUID gradeId) {
        return gradeRepository.findByIdAndDiscipline_Id(gradeId, disciplineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Nota não encontrada."
                ));
    }
}
