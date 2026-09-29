package studdy.example.demo.simulator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.discipline.AcademicPerformance;
import studdy.example.demo.discipline.AcademicPerformanceService;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineAccessService;
import studdy.example.demo.grade.Grade;
import studdy.example.demo.grade.GradeRepository;
import studdy.example.demo.simulator.dto.SimulationStatus;
import studdy.example.demo.simulator.dto.SimulatorRequest;
import studdy.example.demo.simulator.dto.SimulatorResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class SimulatorService {

    private static final int OUTPUT_SCALE = 2;

    private final GradeRepository gradeRepository;
    private final DisciplineAccessService disciplineAccessService;
    private final AcademicPerformanceService academicPerformanceService;

    public SimulatorService(
            GradeRepository gradeRepository,
            DisciplineAccessService disciplineAccessService,
            AcademicPerformanceService academicPerformanceService
    ) {
        this.gradeRepository = gradeRepository;
        this.disciplineAccessService = disciplineAccessService;
        this.academicPerformanceService = academicPerformanceService;
    }

    @Transactional(readOnly = true)
    public SimulatorResponse simulate(
            UUID userId,
            UUID dashboardId,
            UUID disciplineId,
            SimulatorRequest request
    ) {
        // Garante que a disciplina pertence ao usuário.
        Discipline discipline = disciplineAccessService.findOwnedDiscipline(userId, dashboardId, disciplineId);

        List<Grade> grades = gradeRepository.findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(disciplineId);

        // Reutiliza o serviço existente para calcular a média.
        AcademicPerformance performance = academicPerformanceService.calculate(grades, discipline.getPassingAverage());

        // A classificação usa a nota necessária exata; só as saídas são arredondadas. Assim
        // 10,004 continua IMPOSSIBLE mesmo que seja exibido como 10,00.
        BigDecimal exactRequiredScore = calculateExactRequiredScore(grades, request.targetAverage());
        SimulationStatus status = classify(exactRequiredScore);

        // Valor exato arredondado a 2 casas para exibição, sem limitar à faixa [0, 10].
        BigDecimal requiredScoreRaw = exactRequiredScore.setScale(OUTPUT_SCALE, RoundingMode.HALF_UP);

        return new SimulatorResponse(
                performance.average(),
                request.targetAverage(),
                clampToGradeRange(requiredScoreRaw),
                status != SimulationStatus.IMPOSSIBLE,
                requiredScoreRaw,
                status
        );
    }

    /*
     * A classificação depende apenas de a nota necessária estar dentro da faixa válida de uma
     * avaliação [0, 10], independentemente da média de aprovação da disciplina.
     */
    private SimulationStatus classify(BigDecimal exactRequiredScore) {
        if (exactRequiredScore.signum() <= 0) {
            return SimulationStatus.ALREADY_REACHED;
        }
        if (exactRequiredScore.compareTo(BigDecimal.TEN) > 0) {
            return SimulationStatus.IMPOSSIBLE;
        }
        return SimulationStatus.ACHIEVABLE;
    }

    // Campo legado requiredGrade: valor arredondado e limitado (clamped) à faixa 0..10.
    private BigDecimal clampToGradeRange(BigDecimal score) {
        if (score.signum() < 0) {
            return BigDecimal.ZERO.setScale(OUTPUT_SCALE, RoundingMode.HALF_UP);
        }
        if (score.compareTo(BigDecimal.TEN) > 0) {
            return BigDecimal.TEN.setScale(OUTPUT_SCALE, RoundingMode.HALF_UP);
        }
        return score;
    }

    /*
     * (soma atual + nota necessária) / (quantidade atual + 1) = média desejada, logo
     * nota necessária = média desejada * (quantidade + 1) - soma atual.
     * Sem notas anteriores, a primeira nota precisa ser a própria meta. Sem arredondamento.
     */
    private BigDecimal calculateExactRequiredScore(List<Grade> grades, BigDecimal targetAverage) {
        BigDecimal currentSum = grades.stream()
                .map(Grade::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return targetAverage
                .multiply(BigDecimal.valueOf(grades.size() + 1L))
                .subtract(currentSum);
    }
}
