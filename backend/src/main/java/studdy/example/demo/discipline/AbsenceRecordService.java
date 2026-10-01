package studdy.example.demo.discipline;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import studdy.example.demo.discipline.dto.AbsenceRecordResponse;
import studdy.example.demo.discipline.dto.CreateAbsenceRecordRequest;

@Service
public class AbsenceRecordService {

    static final int MAX_TOTAL_ABSENCES = 9999;
    private static final int MAX_HISTORY = 1000;

    private final AbsenceRecordRepository absenceRecordRepository;
    private final FrequencyRepository frequencyRepository;
    private final DisciplineAccessService disciplineAccessService;
    private final DisciplineRepository disciplineRepository;
    private final Clock clock;

    public AbsenceRecordService(
            AbsenceRecordRepository absenceRecordRepository,
            FrequencyRepository frequencyRepository,
            DisciplineAccessService disciplineAccessService,
            DisciplineRepository disciplineRepository,
            Clock clock
    ) {
        this.absenceRecordRepository = absenceRecordRepository;
        this.frequencyRepository = frequencyRepository;
        this.disciplineAccessService = disciplineAccessService;
        this.disciplineRepository = disciplineRepository;
        this.clock = clock;
    }

    @Transactional
    public AbsenceRecordResponse create(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            CreateAbsenceRecordRequest request
    ) {
        // Posse primeiro: quem não é dono recebe 404 sem descobrir nada sobre a disciplina.
        disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId);
        rejectFutureDate(request.date());

        // Trava a disciplina antes do find-or-create da Frequency: na primeira falta ainda não
        // existe linha de Frequency para o lock pessimista, e dois envios simultâneos
        // tentariam inserir duas.
        Discipline discipline = disciplineRepository.findByIdForUpdate(disciplineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Disciplina não encontrada."
                ));
        // Lock pessimista na linha de Frequency da disciplina: garante que o total de
        // faltas seja lido e atualizado atomicamente mesmo com requisições concorrentes
        // para a mesma disciplina (evita divergência entre o histórico e o contador).
        Frequency frequency = frequencyRepository.findByDiscipline_IdForUpdate(disciplineId)
                .orElseGet(() -> new Frequency(discipline, 0));

        BigDecimal previousAttendance = frequency.attendancePercentage();
        frequency.update(addAbsences(frequency.getAbsences(), request.quantity()));
        try {
            frequencyRepository.saveAndFlush(frequency);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A frequência desta disciplina foi alterada por outra requisição. Tente novamente."
            );
        }

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
                .findAllByDiscipline_IdOrderByAbsenceDateDescCreatedAtDesc(
                        disciplineId, PageRequest.of(0, MAX_HISTORY))
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
        Frequency frequency = frequencyRepository.findByDiscipline_IdForUpdate(disciplineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "A frequência desta disciplina ainda não foi cadastrada."
                ));

        frequency.update(Math.max(frequency.getAbsences() - record.getQuantity(), 0));
        frequencyRepository.save(frequency);
        absenceRecordRepository.delete(record);
    }

    private static int addAbsences(int current, int quantity) {
        int total;
        try {
            total = Math.addExact(current, quantity);
        } catch (ArithmeticException exception) {
            throw totalTooHigh();
        }
        if (total > MAX_TOTAL_ABSENCES) {
            throw totalTooHigh();
        }
        return total;
    }

    private static ResponseStatusException totalTooHigh() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "O total de faltas não pode passar de " + MAX_TOTAL_ABSENCES + "."
        );
    }

    private void rejectFutureDate(LocalDate date) {
        if (date.isAfter(LocalDate.now(clock))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Não é possível registrar falta em data futura."
            );
        }
    }
}
