package studdy.example.demo.discipline;

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

        if (frequencyRepository.findByDiscipline_Id(disciplineId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A frequência desta disciplina já foi cadastrada."
            );
        }

        Frequency frequency = frequencyRepository.save(new Frequency(
                discipline,
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

        frequency.update(request.absences());

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
        return new FrequencyResponse(
                frequency.getId(),
                discipline.getId(),
                frequency.getAbsences(),
                frequency.attendancePercentage(),
                FrequencyRules.LOSS_PER_ABSENCE,
                FrequencyRules.maximumAbsences(discipline.getMinimumAttendancePercentage())
        );
    }

    private Frequency findFrequency(UUID disciplineId) {
        return frequencyRepository.findByDiscipline_Id(disciplineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "A frequência desta disciplina ainda não foi cadastrada."
                ));
    }

}
