package studdy.example.demo.grade.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateGradeRequest(
        @NotBlank(message = "O nome da avaliação é obrigatório.")
        @Size(max = 120, message = "O nome da avaliação deve ter no máximo 120 caracteres.")
        String assessmentName,

        @NotNull(message = "A nota é obrigatória.")
        @DecimalMin(value = "0.00", message = "A nota não pode ser negativa.")
        @DecimalMax(value = "10.00", message = "A nota não pode ser maior que 10.")
        @Digits(integer = 2, fraction = 2, message = "A nota deve ter no máximo duas casas decimais.")
        BigDecimal score,

        @NotNull(message = "A data do registro é obrigatória.")
        @PastOrPresent(message = "A data do registro não pode estar no futuro.")
        LocalDate recordedAt
) {
}
