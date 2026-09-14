# Situação da disciplina

A situação representa o ciclo acadêmico informado pelo estudante e não é calculada a partir das notas ou da frequência.

- Toda disciplina nova começa **Em andamento** (`IN_PROGRESS`).
- No botão de três pontos das ações, o estudante pode selecionar **Em andamento**, **Concluída** (`COMPLETED`) ou **Trancada** (`LOCKED`).
- Média e frequência continuam sendo calculadas separadamente. O resultado acadêmico calculado pelas notas é exposto pela API em `performanceStatus` e não altera a situação da disciplina.

Essa escolha manual é necessária porque o sistema não conhece a data real de encerramento ou de trancamento da disciplina.
