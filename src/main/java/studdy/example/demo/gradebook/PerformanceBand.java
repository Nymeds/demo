package studdy.example.demo.gradebook;

import java.math.BigDecimal;

// Faixa de desempenho mostrada na tela Notas, a partir da média parcial da disciplina.
public enum PerformanceBand {
    EXCELLENT,
    GOOD,
    REGULAR,
    INSUFFICIENT,
    NO_GRADES;

    private static final BigDecimal EXCELLENT_FROM = new BigDecimal("9.00");
    private static final BigDecimal GOOD_FROM = new BigDecimal("7.00");
    private static final BigDecimal REGULAR_FROM = new BigDecimal("5.00");

    public static PerformanceBand of(BigDecimal average) {
        if (average == null) {
            return NO_GRADES;
        }

        if (average.compareTo(EXCELLENT_FROM) >= 0) {
            return EXCELLENT;
        }

        if (average.compareTo(GOOD_FROM) >= 0) {
            return GOOD;
        }

        return average.compareTo(REGULAR_FROM) >= 0 ? REGULAR : INSUFFICIENT;
    }
}
