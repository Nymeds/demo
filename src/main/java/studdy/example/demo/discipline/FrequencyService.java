package studdy.example.demo.discipline;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.discipline.dto.CreateFrequencyRequest;
import studdy.example.demo.discipline.dto.FrequencyResponse;
import studdy.example.demo.discipline.dto.UpdateFrequencyRequest;

@Service
public class FrequencyService {

    private final FrequencyRepository frequencyRepository;
    private final DisciplineRepository disciplineRepository;
    private final DashboardRepository dashboardRepository;

    public FrequencyService(
            FrequencyRepository frequencyRepository,
            DisciplineRepository disciplineRepository,
            DashboardRepository dashboardRepository
    ) {
        this.frequencyRepository = frequencyRepository;
        this.disciplineRepository = disciplineRepository;
        this.dashboardRepository = dashboardRepository;
    }

    @Transactional
    public FrequencyResponse create(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            CreateFrequencyRequest request
    ) {
        Dashboard dashboard = dashboardRepository.findByIdAndOwner_Id(
                dashboardId,
                userId
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Dashboard não encontrado."
        ));

        Discipline discipline = disciplineRepository.findByIdAndDashboard_Id(
                disciplineId,
                dashboardId
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Disciplina não encontrada."
        ));

        if (request.absences() > request.totalClasses()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A quantidade de faltas não pode ser maior que a quantidade total de aulas."
            );
        }

        if (frequencyRepository.findByDiscipline_Id(disciplineId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A frequência desta disciplina já foi cadastrada."
            );
        }

        BigDecimal attendancePercentage = BigDecimal.valueOf(
                (double) (request.totalClasses() - request.absences())
                        / request.totalClasses()
                        * 100
        ).setScale(2, RoundingMode.HALF_UP);

        int minimumAttendanceClasses = BigDecimal.valueOf(request.totalClasses())
        .multiply(discipline.getMinimumAttendancePercentage())
        .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING)
        .intValue();

        int maximumAbsences = request.totalClasses() - minimumAttendanceClasses;

        Frequency frequency = new Frequency(
                discipline,
                request.totalClasses(),
                request.absences()
        );

        Frequency savedFrequency = frequencyRepository.save(frequency);

        return new FrequencyResponse(
                savedFrequency.getId(),
                discipline.getId(),
                savedFrequency.getTotalClasses(),
                savedFrequency.getAbsences(),
                attendancePercentage,
                minimumAttendanceClasses,
                maximumAbsences
        );
    }
    @Transactional
    public FrequencyResponse update(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            UpdateFrequencyRequest request
    ) {
        Dashboard dashboard = dashboardRepository.findByIdAndOwner_Id(
                dashboardId,
                userId
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Dashboard não encontrado."
        ));

        Discipline discipline = disciplineRepository.findByIdAndDashboard_Id(
                disciplineId,
                dashboardId
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Disciplina não encontrada."
        ));

        Frequency frequency = frequencyRepository.findByDiscipline_Id(disciplineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "A frequência desta disciplina ainda não foi cadastrada."
                ));
        if (request.absences() > request.totalClasses()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A quantidade de faltas não pode ser maior que a quantidade total de aulas."
            );
        }

        BigDecimal attendancePercentage = BigDecimal.valueOf(
                (double) (request.totalClasses() - request.absences())
                        / request.totalClasses()
                        * 100
        ).setScale(2, RoundingMode.HALF_UP);

        int minimumAttendanceClasses = BigDecimal.valueOf(request.totalClasses())
        .multiply(discipline.getMinimumAttendancePercentage())
        .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING)
        .intValue();

        int maximumAbsences = request.totalClasses() - minimumAttendanceClasses;

        frequency.update(
                request.totalClasses(),
                request.absences()
        );

        Frequency updatedFrequency = frequencyRepository.save(frequency);

        return new FrequencyResponse(
                updatedFrequency.getId(),
                discipline.getId(),
                updatedFrequency.getTotalClasses(),
                updatedFrequency.getAbsences(),
                attendancePercentage,
                minimumAttendanceClasses,
                maximumAbsences
        );
    }
    @Transactional(readOnly = true)
public FrequencyResponse findByDiscipline(
        UUID userId,
        UUID dashboardId,
        UUID disciplineId
) {
    dashboardRepository.findByIdAndOwner_Id(
            dashboardId,
            userId
    ).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Dashboard não encontrado."
    ));

    Discipline discipline = disciplineRepository.findByIdAndDashboard_Id(
            disciplineId,
            dashboardId
    ).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Disciplina não encontrada."
    ));

    Frequency frequency = frequencyRepository.findByDiscipline_Id(disciplineId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "A frequência desta disciplina ainda não foi cadastrada."
            ));

    BigDecimal attendancePercentage = BigDecimal.valueOf(
            (double) (frequency.getTotalClasses() - frequency.getAbsences())
                    / frequency.getTotalClasses()
                    * 100
    ).setScale(2, RoundingMode.HALF_UP);

    int minimumAttendanceClasses = BigDecimal.valueOf(frequency.getTotalClasses())
        .multiply(discipline.getMinimumAttendancePercentage())
        .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING)
        .intValue();

    int maximumAbsences = frequency.getTotalClasses() - minimumAttendanceClasses;

    return new FrequencyResponse(
            frequency.getId(),
            discipline.getId(),
            frequency.getTotalClasses(),
            frequency.getAbsences(),
            attendancePercentage,
            minimumAttendanceClasses,
            maximumAbsences
    );
}
}