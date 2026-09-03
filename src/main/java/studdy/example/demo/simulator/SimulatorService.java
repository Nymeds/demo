package studdy.example.demo.simulator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.discipline.AcademicPerformance;
import studdy.example.demo.discipline.AcademicPerformanceService;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineAccessService;
import studdy.example.demo.grade.Grade;
import studdy.example.demo.grade.GradeRepository;
import studdy.example.demo.simulator.dto.SimulatorRequest;
import studdy.example.demo.simulator.dto.SimulatorResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class SimulatorService {


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
    Discipline discipline = disciplineAccessService.findOwnedDiscipline(
            userId,
            dashboardId,
            disciplineId
    );

    List<Grade> grades =
            gradeRepository.findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(
                    disciplineId
            );

    // Reutiliza o serviço existente para calcular a média.
    AcademicPerformance performance =
            academicPerformanceService.calculate(
                    grades,
                    discipline.getPassingAverage()
            );

    BigDecimal currentAverage = performance.average();

    BigDecimal requiredGrade = calculateRequiredGrade(
            grades.size(),
            currentAverage,
            request.targetAverage()
    );

    /*
     * A nota necessária é atingível quando está entre 0 e 10
     * e a meta respeita a média de aprovação da disciplina.
     */
    boolean achievable =
            requiredGrade.compareTo(BigDecimal.ZERO) >= 0
                    && requiredGrade.compareTo(BigDecimal.TEN) <= 0
                    && request.targetAverage().compareTo(
                            discipline.getPassingAverage()
                    ) >= 0;

    /*
     * Mantém a nota retornada dentro da faixa válida de 0 a 10.
     * O campo "achievable" informa se a nota original era possível.
     */
    if (requiredGrade.compareTo(BigDecimal.ZERO) < 0) {
        requiredGrade = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    } else if (requiredGrade.compareTo(BigDecimal.TEN) > 0) {
        requiredGrade = BigDecimal.TEN.setScale(2, RoundingMode.HALF_UP);
    }

    return new SimulatorResponse(
            currentAverage,
            request.targetAverage(),
            requiredGrade,
            achievable
    );
}

private BigDecimal calculateRequiredGrade(
        int gradeCount,
        BigDecimal currentAverage,
        BigDecimal targetAverage
) {

    // Sem notas anteriores, a primeira nota precisa ser a própria meta.
    if (gradeCount == 0) {
        return targetAverage.setScale(2, RoundingMode.HALF_UP);
    }

    /*
     * Recupera a soma das notas usando a média já calculada.
     * Assim evitamos percorrer a lista de notas novamente.
     */
    BigDecimal currentSum = currentAverage.multiply(
            BigDecimal.valueOf(gradeCount)
    );

    /*
     * Fórmula:
     *
     * (soma atual + nota necessária) / (quantidade atual + 1)
     * = média desejada
     *
     * Logo:
     *
     * nota necessária =
     * média desejada * (quantidade + 1) - soma atual
     */
    BigDecimal targetTotal = targetAverage.multiply(
            BigDecimal.valueOf(gradeCount + 1)
    );

    BigDecimal requiredGrade = targetTotal.subtract(currentSum);

    return requiredGrade.setScale(2, RoundingMode.HALF_UP);
}
}
