package studdy.example.demo.discipline;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.discipline.dto.ClassScheduleRequest;
import studdy.example.demo.discipline.dto.CreateDisciplineRequest;
import studdy.example.demo.discipline.dto.DisciplineResponse;
import studdy.example.demo.discipline.dto.UpdateDisciplineRequest;

import java.util.List;
import java.util.UUID;

@Service
public class DisciplineService {

    private final DisciplineRepository disciplineRepository;
    private final DisciplineAccessService disciplineAccessService;
    private final AcademicPerformanceService academicPerformanceService;

    public DisciplineService(
            DisciplineRepository disciplineRepository,
            DisciplineAccessService disciplineAccessService,
            AcademicPerformanceService academicPerformanceService
    ) {
        this.disciplineRepository = disciplineRepository;
        this.disciplineAccessService = disciplineAccessService;
        this.academicPerformanceService = academicPerformanceService;
    }

    @Transactional
    public DisciplineResponse create(
            UUID userId,
            UUID dashboardId,
            CreateDisciplineRequest request
    ) {
        Dashboard dashboard = disciplineAccessService.findOwnedDashboard(userId, dashboardId);

        Discipline discipline = new Discipline(
                request.name().trim(),
                request.professorName().trim(),
                request.workloadHours(),
                request.passingAverage(),
                dashboard,
                toSchedules(request.schedules())
        );

        return toResponse(disciplineRepository.save(discipline));
    }

    @Transactional(readOnly = true)
    public List<DisciplineResponse> findAll(UUID userId, UUID dashboardId) {
        disciplineAccessService.findOwnedDashboard(userId, dashboardId);

        return disciplineRepository.findAllByDashboard_IdOrderByNameAsc(dashboardId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DisciplineResponse findById(UUID userId, UUID dashboardId, UUID disciplineId) {
        return toResponse(disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId));
    }

    @Transactional
    public DisciplineResponse update(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            UpdateDisciplineRequest request
    ) {
        Discipline discipline = disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId);

        discipline.update(
                request.name().trim(),
                request.professorName().trim(),
                request.workloadHours(),
                request.passingAverage(),
                toSchedules(request.schedules())
        );

        return toResponse(discipline);
    }

    @Transactional
    public void delete(UUID userId, UUID dashboardId, UUID disciplineId) {
        disciplineRepository.delete(disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId));
    }

    private DisciplineResponse toResponse(Discipline discipline) {
        return DisciplineResponse.from(
                discipline,
                academicPerformanceService.calculate(discipline.getGrades(), discipline.getPassingAverage())
        );
    }

    private List<ClassSchedule> toSchedules(List<ClassScheduleRequest> requests) {
        return requests.stream()
                .map(request -> new ClassSchedule(
                        request.dayOfWeek(),
                        request.startTime(),
                        request.endTime()
                ))
                .toList();
    }
}
