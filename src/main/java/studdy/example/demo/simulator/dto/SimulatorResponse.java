package studdy.example.demo.simulator.dto;

import java.math.BigDecimal;

/**
 * @param currentAverage  média atual da disciplina (escala 2, HALF_UP).
 * @param targetAverage   média desejada, ecoada da requisição.
 * @param requiredGrade   nota necessária, mantida para compatibilidade com
 *                        clientes existentes: arredondada (escala 2, HALF_UP)
 *                        e limitada (clamped) à faixa [0, 10]. Para o valor
 *                        real sem limitação, use {@link #requiredScoreRaw()}.
 * @param achievable      true quando a nota necessária (sem limitação) está
 *                        dentro de [0, 10], independentemente da média de
 *                        aprovação da disciplina.
 * @param requiredScoreRaw nota necessária real, arredondada apenas para
 *                          apresentação (escala 2, HALF_UP) mas NÃO limitada
 *                          à faixa [0, 10]. Pode ser negativa (meta já
 *                          atingida) ou maior que 10 (meta impossível).
 * @param status          classificação da simulação: ALREADY_REACHED,
 *                        ACHIEVABLE ou IMPOSSIBLE.
 */
public record SimulatorResponse(
        BigDecimal currentAverage,
        BigDecimal targetAverage,
        BigDecimal requiredGrade,
        boolean achievable,
        BigDecimal requiredScoreRaw,
        SimulationStatus status
) {
}
