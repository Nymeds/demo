package studdy.example.demo.gradebook;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerformanceBandTest {

    @Test
    void classifiesTheEdgesOfEachBand() {
        assertEquals(PerformanceBand.EXCELLENT, PerformanceBand.of(new BigDecimal("10.00")));
        assertEquals(PerformanceBand.EXCELLENT, PerformanceBand.of(new BigDecimal("9.00")));
        assertEquals(PerformanceBand.EXCELLENT, PerformanceBand.of(new BigDecimal("8.99"))); // Rounds to 9.0
        assertEquals(PerformanceBand.GOOD, PerformanceBand.of(new BigDecimal("8.94"))); // Rounds to 8.9
        assertEquals(PerformanceBand.GOOD, PerformanceBand.of(new BigDecimal("7.00")));
        assertEquals(PerformanceBand.GOOD, PerformanceBand.of(new BigDecimal("6.99"))); // Rounds to 7.0
        assertEquals(PerformanceBand.REGULAR, PerformanceBand.of(new BigDecimal("6.94"))); // Rounds to 6.9
        assertEquals(PerformanceBand.REGULAR, PerformanceBand.of(new BigDecimal("5.00")));
        assertEquals(PerformanceBand.REGULAR, PerformanceBand.of(new BigDecimal("4.99"))); // Rounds to 5.0
        assertEquals(PerformanceBand.INSUFFICIENT, PerformanceBand.of(new BigDecimal("4.94"))); // Rounds to 4.9
        assertEquals(PerformanceBand.INSUFFICIENT, PerformanceBand.of(BigDecimal.ZERO));
    }

    @Test
    void marksADisciplineWithoutGrades() {
        assertEquals(PerformanceBand.NO_GRADES, PerformanceBand.of(null));
    }

    @Test
    void classifiesUnroundedAveragesAfterRoundingToOneDecimal() {
        // 6.96 rounds to 7.0 (HALF_UP), should be GOOD (same as 7.0)
        assertEquals(PerformanceBand.GOOD, PerformanceBand.of(new BigDecimal("6.96")));
        assertEquals(PerformanceBand.GOOD, PerformanceBand.of(new BigDecimal("7.00")));

        // 6.94 rounds to 6.9, should be REGULAR (same as 6.9)
        assertEquals(PerformanceBand.REGULAR, PerformanceBand.of(new BigDecimal("6.94")));
        assertEquals(PerformanceBand.REGULAR, PerformanceBand.of(new BigDecimal("6.90")));

        // 8.96 rounds to 9.0, should be EXCELLENT (same as 9.0)
        assertEquals(PerformanceBand.EXCELLENT, PerformanceBand.of(new BigDecimal("8.96")));
        assertEquals(PerformanceBand.EXCELLENT, PerformanceBand.of(new BigDecimal("9.00")));

        // 4.94 rounds to 4.9, should be INSUFFICIENT (same as 4.9)
        assertEquals(PerformanceBand.INSUFFICIENT, PerformanceBand.of(new BigDecimal("4.94")));
        assertEquals(PerformanceBand.INSUFFICIENT, PerformanceBand.of(new BigDecimal("4.90")));

        // 4.96 rounds to 5.0, should be REGULAR (same as 5.0)
        assertEquals(PerformanceBand.REGULAR, PerformanceBand.of(new BigDecimal("4.96")));
        assertEquals(PerformanceBand.REGULAR, PerformanceBand.of(new BigDecimal("5.00")));

        // 6.95 rounds to 7.0, should be GOOD (same as 7.0)
        assertEquals(PerformanceBand.GOOD, PerformanceBand.of(new BigDecimal("6.95")));
    }
}
