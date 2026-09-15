package studdy.example.demo.gradebook;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerformanceBandTest {

    @Test
    void classifiesTheEdgesOfEachBand() {
        assertEquals(PerformanceBand.EXCELLENT, PerformanceBand.of(new BigDecimal("10.00")));
        assertEquals(PerformanceBand.EXCELLENT, PerformanceBand.of(new BigDecimal("9.00")));
        assertEquals(PerformanceBand.GOOD, PerformanceBand.of(new BigDecimal("8.99")));
        assertEquals(PerformanceBand.GOOD, PerformanceBand.of(new BigDecimal("7.00")));
        assertEquals(PerformanceBand.REGULAR, PerformanceBand.of(new BigDecimal("6.99")));
        assertEquals(PerformanceBand.REGULAR, PerformanceBand.of(new BigDecimal("5.00")));
        assertEquals(PerformanceBand.INSUFFICIENT, PerformanceBand.of(new BigDecimal("4.99")));
        assertEquals(PerformanceBand.INSUFFICIENT, PerformanceBand.of(BigDecimal.ZERO));
    }

    @Test
    void marksADisciplineWithoutGrades() {
        assertEquals(PerformanceBand.NO_GRADES, PerformanceBand.of(null));
    }
}
