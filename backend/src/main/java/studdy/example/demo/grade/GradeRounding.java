package studdy.example.demo.grade;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento único da média exibida ao estudante (1 casa, HALF_UP). A decisão de
 * aprovação e a faixa de desempenho usam este mesmo valor, para que a tela nunca mostre,
 * por exemplo, "7,0" numa disciplina marcada como reprovada por 6,95.
 */
public final class GradeRounding {

    public static final int DISPLAY_SCALE = 1;

    private GradeRounding() {
    }

    public static BigDecimal displayAverage(BigDecimal average) {
        return average.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }
}
