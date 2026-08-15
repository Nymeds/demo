package studdy.example.demo.discipline;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.discipline.dto.ClassScheduleRequest;
import studdy.example.demo.discipline.dto.CreateDisciplineRequest;
import studdy.example.demo.discipline.dto.DisciplineResponse;
import studdy.example.demo.discipline.dto.UpdateDisciplineRequest;

@Service
public class DisciplineService {

    private final DisciplineRepository disciplineRepository;
    private final DashboardRepository dashboardRepository;

    public DisciplineService(
            DisciplineRepository disciplineRepository,
            DashboardRepository dashboardRepository
    ) {
        this.disciplineRepository = disciplineRepository;
        this.dashboardRepository = dashboardRepository;
    }

    @Transactional
    public DisciplineResponse create(
            UUID userId,
            UUID dashboardId,
            CreateDisciplineRequest request
    ) {
        Dashboard dashboard = findOwnedDashboard(userId, dashboardId);

        Discipline discipline = new Discipline(
                request.name().trim(),
                request.professorName().trim(),
                request.workloadHours(),
                request.minimumAttendancePercentage(),
                dashboard,
                toSchedules(request.schedules())
        );

        return DisciplineResponse.from(disciplineRepository.save(discipline));
    }

    @Transactional(readOnly = true)
    public List<DisciplineResponse> findAll(UUID userId, UUID dashboardId) {
        findOwnedDashboard(userId, dashboardId);

        return disciplineRepository.findAllByDashboard_IdOrderByNameAsc(dashboardId).stream()
                .map(DisciplineResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DisciplineResponse findById(UUID userId, UUID dashboardId, UUID disciplineId) {
        return DisciplineResponse.from(findOwnedDiscipline(userId, dashboardId, disciplineId));
    }

    @Transactional
    public DisciplineResponse update(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            UpdateDisciplineRequest request
    ) {
        Discipline discipline = findOwnedDiscipline(userId, dashboardId, disciplineId);

        discipline.update(
                request.name().trim(),
                request.professorName().trim(),
                request.workloadHours(),
                request.minimumAttendancePercentage(),
                toSchedules(request.schedules())
        );

        return DisciplineResponse.from(discipline);
    }

    @Transactional
    public void delete(UUID userId, UUID dashboardId, UUID disciplineId) {
        disciplineRepository.delete(findOwnedDiscipline(userId, dashboardId, disciplineId));
    }

    private Dashboard findOwnedDashboard(UUID userId, UUID dashboardId) {
        return dashboardRepository.findByIdAndOwner_Id(dashboardId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Dashboard não encontrado."
                ));
    }

    private Discipline findOwnedDiscipline(UUID userId, UUID dashboardId, UUID disciplineId) {
        findOwnedDashboard(userId, dashboardId);

        return disciplineRepository.findByIdAndDashboard_Id(disciplineId, dashboardId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Disciplina não encontrada."
                ));
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
