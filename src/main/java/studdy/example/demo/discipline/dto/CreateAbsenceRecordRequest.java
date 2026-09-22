package studdy.example.demo.discipline.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateAbsenceRecordRequest(
        @NotNull(message = "A data da falta é obrigatória.")
        LocalDate date,

        @NotNull(message = "A quantidade de faltas é obrigatória.")
        @Positive(message = "A quantidade de faltas deve ser maior que zero.")
        Integer quantity,

        @NotBlank(message = "O motivo da falta é obrigatório.")
        @Size(max = 40, message = "O motivo deve ter no máximo 40 caracteres.")
        String reason,

        @Size(max = 300, message = "A observação deve ter no máximo 300 caracteres.")
        String note
) {
}
