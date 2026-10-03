package studdy.example.demo.gradebook;

import java.math.BigDecimal;
import studdy.example.demo.grade.GradeRounding;

// Faixa de desempenho mostrada na tela Notas, a partir da média parcial da disciplina.
public enum PerformanceBand {
    EXCELLENT,
    GOOD,
    REGULAR,
    INSUFFICIENT,
    NO_GRADES;

    private static final BigDecimal EXCELLENT_FROM = new BigDecimal("9.0");
    private static final BigDecimal GOOD_FROM = new BigDecimal("7.0");
    private static final BigDecimal REGULAR_FROM = new BigDecimal("5.0");

    public static PerformanceBand of(BigDecimal average) {
        if (average == null) {
            return NO_GRADES;
        }

        // Mesmo arredondamento usado na decisão de aprovação
        BigDecimal roundedAverage = GradeRounding.displayAverage(average);

        if (roundedAverage.compareTo(EXCELLENT_FROM) >= 0) {
            return EXCELLENT;
        }

        if (roundedAverage.compareTo(GOOD_FROM) >= 0) {
            return GOOD;
        }

        return roundedAverage.compareTo(REGULAR_FROM) >= 0 ? REGULAR : INSUFFICIENT;
    }
}
