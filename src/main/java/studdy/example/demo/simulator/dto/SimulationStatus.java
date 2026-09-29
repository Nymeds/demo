package studdy.example.demo.simulator.dto;

/**
 * Classifica a nota necessária calculada pelo simulador.
 *
 * A classificação depende apenas de a nota necessária (não arredondada
 * para a faixa 0..10) estar dentro do intervalo válido de uma avaliação,
 * independentemente da média de aprovação da disciplina.
 */
public enum SimulationStatus {

    /** A nota necessária é menor ou igual a zero: a meta já foi atingida. */
    ALREADY_REACHED,

    /** A nota necessária está dentro do intervalo [0, 10]. */
    ACHIEVABLE,

    /** A nota necessária é maior que 10: não é possível atingir a meta. */
    IMPOSSIBLE
}
