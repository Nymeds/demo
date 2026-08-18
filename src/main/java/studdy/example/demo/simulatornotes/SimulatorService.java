package studdy.example.demo.simulatornotes;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.discipline.DisciplineAccessService;
import studdy.example.demo.grade.Grade;
import studdy.example.demo.grade.GradeRepository;
import studdy.example.demo.simulatornotes.dto.SimulatorRequest;
import studdy.example.demo.simulatornotes.dto.SimulatorResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class SimulatorService {

    private final GradeRepository gradeRepository;
    private final DisciplineAccessService disciplineAccessService;

    public SimulatorService(
            GradeRepository gradeRepository,
            DisciplineAccessService disciplineAccessService
    ) {
        this.gradeRepository = gradeRepository;
        this.disciplineAccessService = disciplineAccessService;
    }

    @Transactional(readOnly = true)
    public SimulatorResponse simulate(
            UUID userId,
            UUID dashboardId,
            SimulatorRequest request
    ) {

        // Verifica se o usuário possui acesso à disciplina.
        disciplineAccessService.findOwnedDiscipline(
                userId,
                dashboardId,
                request.disciplineId()
        );

        // Busca as notas já cadastradas para a disciplina.
        List<Grade> grades =
                gradeRepository.findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(
                        request.disciplineId()
                );

        // Calcula a média atual.
        BigDecimal currentAverage = calculateAverage(grades);

        // Calcula a nota necessária para alcançar a média desejada.
        BigDecimal requiredGrade = calculateRequiredGrade(
                grades,
                request.targetAverage()
        );

        return new SimulatorResponse(
                request.disciplineId(),
                currentAverage,
                request.targetAverage(),
                requiredGrade
        );
    }

    private BigDecimal calculateAverage(List<Grade> grades) {

        if (grades.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal sum = BigDecimal.ZERO;

        for (Grade grade : grades) {
            sum = sum.add(grade.getScore());
        }

        return sum.divide(
                BigDecimal.valueOf(grades.size()),
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal calculateRequiredGrade(
            List<Grade> grades,
            BigDecimal targetAverage
    ) {

        BigDecimal currentSum = BigDecimal.ZERO;

        for (Grade grade : grades) {
            currentSum = currentSum.add(grade.getScore());
        }

        int futureGradeCount = grades.size() + 1;

        BigDecimal targetTotal = targetAverage.multiply(
                BigDecimal.valueOf(futureGradeCount)
        );

        BigDecimal requiredGrade = targetTotal.subtract(currentSum);

        return requiredGrade.setScale(2, RoundingMode.HALF_UP);
    }
}