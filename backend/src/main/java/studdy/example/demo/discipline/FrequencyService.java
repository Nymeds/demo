package studdy.example.demo.discipline;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
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
    private final DisciplineRepository disciplineRepository;

    public FrequencyService(
            FrequencyRepository frequencyRepository,
            DisciplineAccessService disciplineAccessService,
            DisciplineRepository disciplineRepository
    ) {
        this.frequencyRepository = frequencyRepository;
        this.disciplineAccessService = disciplineAccessService;
        this.disciplineRepository = disciplineRepository;
    }

    @Transactional
    public FrequencyResponse create(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            CreateFrequencyRequest request
    ) {
        disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId);
        // Trava a disciplina para serializar o find-or-create com AbsenceRecordService.create.
        Discipline discipline = disciplineRepository.findByIdForUpdate(disciplineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Disciplina não encontrada."
                ));

        if (frequencyRepository.findByDiscipline_Id(disciplineId).isPresent()) {
            throw frequencyAlreadyRegistered();
        }

        Frequency frequency;
        try {
            frequency = frequencyRepository.saveAndFlush(new Frequency(
                    discipline,
                    request.absences()
            ));
        } catch (DataIntegrityViolationException exception) {
            // Rede de segurança: a restrição uk_frequency_discipline_id cobre qualquer caminho
            // que escape do lock. Nada mais deve tocar o banco depois desta falha.
            throw frequencyAlreadyRegistered();
        }

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
        // Lock pessimista: uma edição manual do total não pode se perder (nem
        // sobrescrever silenciosamente) uma alteração concorrente vinda do
        // histórico de faltas (create/delete de AbsenceRecord).
        Frequency frequency = frequencyRepository.findByDiscipline_IdForUpdate(disciplineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "A frequência desta disciplina ainda não foi cadastrada."
                ));

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

    private ResponseStatusException frequencyAlreadyRegistered() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A frequência desta disciplina já foi cadastrada."
        );
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
