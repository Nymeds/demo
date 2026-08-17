package studdy.example.demo.discipline;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import studdy.example.demo.discipline.dto.CreateFrequencyRequest;
import studdy.example.demo.discipline.dto.FrequencyResponse;
import studdy.example.demo.discipline.dto.UpdateFrequencyRequest;

@Service
public class FrequencyService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final FrequencyRepository frequencyRepository;
    private final DisciplineAccessService disciplineAccessService;

    public FrequencyService(
            FrequencyRepository frequencyRepository,
            DisciplineAccessService disciplineAccessService
    ) {
        this.frequencyRepository = frequencyRepository;
        this.disciplineAccessService = disciplineAccessService;
    }

    @Transactional
    public FrequencyResponse create(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            CreateFrequencyRequest request
    ) {
        Discipline discipline = disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId);

        validateAbsences(request.totalClasses(), request.absences());

        if (frequencyRepository.findByDiscipline_Id(disciplineId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A frequência desta disciplina já foi cadastrada."
            );
        }

        Frequency frequency = frequencyRepository.save(new Frequency(
                discipline,
                request.totalClasses(),
                request.absences()
        ));

        return toResponse(discipline, frequency);
    }

    @Transactional
    public FrequencyResponse update(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            UpdateFrequencyRequest request
    ) {
        Discipline discipline = disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId);
        Frequency frequency = findFrequency(disciplineId);

        validateAbsences(request.totalClasses(), request.absences());

        frequency.update(request.totalClasses(), request.absences());

        return toResponse(discipline, frequencyRepository.save(frequency));
    }

    @Transactional(readOnly = true)
    public FrequencyResponse findByDiscipline(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId
    ) {
        Discipline discipline = disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId);

        return toResponse(discipline, findFrequency(disciplineId));
    }

    private FrequencyResponse toResponse(Discipline discipline, Frequency frequency) {
        int totalClasses = frequency.getTotalClasses();
        int attendedClasses = totalClasses - frequency.getAbsences();

        BigDecimal attendancePercentage = BigDecimal.valueOf(attendedClasses)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(totalClasses), 2, RoundingMode.HALF_UP);

        // Arredonda para cima: cursar meia aula a menos não cumpre a exigência da disciplina.
        int minimumAttendanceClasses = BigDecimal.valueOf(totalClasses)
                .multiply(discipline.getMinimumAttendancePercentage())
                .divide(ONE_HUNDRED, 0, RoundingMode.CEILING)
                .intValue();

        return new FrequencyResponse(
                frequency.getId(),
                discipline.getId(),
                totalClasses,
                frequency.getAbsences(),
                attendancePercentage,
                minimumAttendanceClasses,
                totalClasses - minimumAttendanceClasses
        );
    }

    private Frequency findFrequency(UUID disciplineId) {
        return frequencyRepository.findByDiscipline_Id(disciplineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "A frequência desta disciplina ainda não foi cadastrada."
                ));
    }

    private void validateAbsences(Integer totalClasses, Integer absences) {
        if (absences > totalClasses) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A quantidade de faltas não pode ser maior que a quantidade total de aulas."
            );
        }
    }
}
