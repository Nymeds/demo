# Regra de frequência

A frequência de cada disciplina começa em 100%. Cada falta registrada reduz 5%, com limite mínimo de 0%. Assim, cinco faltas resultam em 75% e seis faltas resultam em 70%.

O sistema não solicita nem estima o total de aulas do período. O percentual é uma orientação baseada na regra fixa acima e não substitui o registro oficial da instituição. A frequência mínima exigida permanece configurável por disciplina e tem valor inicial de 75%.

Cada lançamento de falta é persistido com data, disciplina, quantidade, motivo e observação. O histórico permanece disponível depois de atualizar a página ou iniciar uma nova sessão.

O total inicial de faltas pode ser informado pela API de frequência antes do primeiro lançamento no histórico. Depois que existe um lançamento, alterações no total devem ser feitas pelo registro ou pela exclusão desse lançamento. O `PUT` da frequência aceita repetir o valor atual, usado ao salvar a configuração sem alterar as faltas, mas devolve `409` quando tenta substituí-lo por outro valor.
