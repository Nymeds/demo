package studdy.example.demo.discipline;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FrequencyRules {
    public static final BigDecimal LOSS_PER_ABSENCE = BigDecimal.valueOf(5);

    private FrequencyRules() {
    }

    public static int maximumAbsences(BigDecimal minimumAttendancePercentage) {
        return BigDecimal.valueOf(100).subtract(minimumAttendancePercentage)
                .divide(LOSS_PER_ABSENCE, 0, RoundingMode.FLOOR).intValueExact();
    }
}
