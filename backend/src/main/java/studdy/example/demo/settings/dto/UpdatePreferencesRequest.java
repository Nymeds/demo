package studdy.example.demo.settings.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import studdy.example.demo.settings.StartSection;

import java.math.BigDecimal;

public record UpdatePreferencesRequest(
        @NotNull(message = "Informe com quantos dias de antecedência quer ser avisado dos prazos.")
        @Min(value = 1, message = "A antecedência dos avisos de prazo deve ser de pelo menos 1 dia.")
        @Max(value = 30, message = "A antecedência dos avisos de prazo deve ser de no máximo 30 dias.")
        Integer deadlineAlertDays,

        @NotNull(message = "Informe a margem do aviso de frequência.")
        @Min(value = 0, message = "A margem do aviso de frequência não pode ser negativa.")
        @Max(value = 30, message = "A margem do aviso de frequência deve ser de no máximo 30 pontos.")
        Integer attendanceAlertMargin,

        @NotNull(message = "Escolha a tela inicial.")
        StartSection startSection,

        // Opcional: vazia quando o estudante ainda não definiu uma meta.
        @DecimalMin(value = "0.0", message = "A meta de média não pode ser negativa.")
        @DecimalMax(value = "10.0", message = "A meta de média deve ser de no máximo 10.")
        @Digits(integer = 2, fraction = 1, message = "Use no máximo uma casa decimal na meta de média.")
        BigDecimal gradeGoal
) {
}
