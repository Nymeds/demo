package studdy.example.demo.discipline.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateDisciplineRequest(
        @NotBlank(message = "O nome da disciplina é obrigatório.")
        @Size(max = 120, message = "O nome da disciplina deve ter no máximo 120 caracteres.")
        String name,

        @NotBlank(message = "O nome do professor é obrigatório.")
        @Size(max = 120, message = "O nome do professor deve ter no máximo 120 caracteres.")
        String professorName,

        @NotNull(message = "A carga horária é obrigatória.")
        @Positive(message = "A carga horária deve ser maior que zero.")
        Integer workloadHours,

        @NotNull(message = "A média de aprovação é obrigatória.")
        @DecimalMin(value = "0.00", message = "A média de aprovação não pode ser negativa.")
        @DecimalMax(value = "10.00", message = "A média de aprovação não pode ser maior que 10.")
        @Digits(integer = 2, fraction = 2, message = "A média de aprovação deve ter no máximo duas casas decimais.")
        BigDecimal passingAverage,

        @NotNull(message = "O percentual mínimo de frequência é obrigatório.")
        @DecimalMin(value = "0.0", message = "O percentual mínimo de frequência não pode ser menor que 0.")
        @DecimalMax(value = "100.0", message = "O percentual mínimo de frequência não pode ser maior que 100.")
        BigDecimal minimumAttendancePercentage,

        @NotNull(message = "A lista de horários é obrigatória.")
        @Size(min = 1, message = "A disciplina deve ter pelo menos um horário.")
        List<@Valid ClassScheduleRequest> schedules
) {
}
