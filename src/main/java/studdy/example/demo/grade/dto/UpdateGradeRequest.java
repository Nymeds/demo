package studdy.example.demo.grade.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateGradeRequest(
        @NotBlank(message = "O nome da avaliação é obrigatório.")
        @Size(max = 120, message = "O nome da avaliação deve ter no máximo 120 caracteres.")
        String assessmentName,

        @NotNull(message = "A nota é obrigatória.")
        @DecimalMin(value = "0.00", message = "A nota não pode ser negativa.")
        BigDecimal score,

        @NotNull(message = "O peso da avaliação é obrigatório.")
        @DecimalMin(value = "0.01", message = "O peso deve ser maior que zero.")
        BigDecimal weight,

        @NotNull(message = "A data do registro é obrigatória.")
        @PastOrPresent(message = "A data do registro não pode estar no futuro.")
        LocalDate recordedAt
) {
}
