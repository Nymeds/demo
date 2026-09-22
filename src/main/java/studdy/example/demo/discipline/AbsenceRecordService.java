package studdy.example.demo.discipline;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import studdy.example.demo.discipline.dto.AbsenceRecordResponse;
import studdy.example.demo.discipline.dto.CreateAbsenceRecordRequest;

@Service
public class AbsenceRecordService {

    private final AbsenceRecordRepository absenceRecordRepository;
    private final FrequencyRepository frequencyRepository;
    private final DisciplineAccessService disciplineAccessService;

    public AbsenceRecordService(
            AbsenceRecordRepository absenceRecordRepository,
            FrequencyRepository frequencyRepository,
            DisciplineAccessService disciplineAccessService
    ) {
        this.absenceRecordRepository = absenceRecordRepository;
        this.frequencyRepository = frequencyRepository;
        this.disciplineAccessService = disciplineAccessService;
    }

    @Transactional
    public AbsenceRecordResponse create(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            CreateAbsenceRecordRequest request
    ) {
        Discipline discipline = disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId);
        Frequency frequency = frequencyRepository.findByDiscipline_Id(disciplineId)
                .orElseGet(() -> new Frequency(discipline, 0));

        BigDecimal previousAttendance = frequency.attendancePercentage();
        frequency.update(frequency.getAbsences() + request.quantity());
        frequencyRepository.save(frequency);

        BigDecimal impact = previousAttendance.subtract(frequency.attendancePercentage());
        AbsenceRecord record = absenceRecordRepository.save(new AbsenceRecord(
                discipline,
                request.date(),
                request.quantity(),
                request.reason(),
                request.note(),
                impact
        ));

        return AbsenceRecordResponse.from(record);
    }

    @Transactional(readOnly = true)
    public List<AbsenceRecordResponse> findAll(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId
    ) {
        disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId);

        return absenceRecordRepository
                .findAllByDiscipline_IdOrderByAbsenceDateDescCreatedAtDesc(disciplineId)
                .stream()
                .map(AbsenceRecordResponse::from)
                .toList();
    }

    @Transactional
    public void delete(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            UUID recordId
    ) {
        disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId);
        AbsenceRecord record = absenceRecordRepository.findByIdAndDiscipline_Id(recordId, disciplineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Lançamento de falta não encontrado."
                ));
        Frequency frequency = frequencyRepository.findByDiscipline_Id(disciplineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "A frequência desta disciplina ainda não foi cadastrada."
                ));

        frequency.update(Math.max(frequency.getAbsences() - record.getQuantity(), 0));
        frequencyRepository.save(frequency);
        absenceRecordRepository.delete(record);
    }
}
