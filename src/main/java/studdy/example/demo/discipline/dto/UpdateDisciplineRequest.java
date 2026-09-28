package studdy.example.demo.discipline.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateDisciplineRequest(
        @NotBlank(message = "O nome da disciplina é obrigatório.")
        @Size(max = 120, message = "O nome da disciplina deve ter no máximo 120 caracteres.")
        String name,

        @Size(max = 120, message = "O nome do professor deve ter no máximo 120 caracteres.")
        String professorName,

        @NotBlank(message = "A cor da disciplina é obrigatória.")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "A cor da disciplina deve estar no formato hexadecimal.")
        String color,

        @NotNull(message = "A média de aprovação é obrigatória.")
        @DecimalMin(value = "0.00", message = "A média de aprovação não pode ser negativa.")
        @DecimalMax(value = "10.00", message = "A média de aprovação não pode ser maior que 10.")
        @Digits(integer = 2, fraction = 2, message = "A média de aprovação deve ter no máximo duas casas decimais.")
        BigDecimal passingAverage,

        @NotNull(message = "O percentual mínimo de frequência é obrigatório.")
        @DecimalMin(value = "0.0", message = "O percentual mínimo de frequência não pode ser menor que 0.")
        @DecimalMax(value = "100.0", message = "O percentual mínimo de frequência não pode ser maior que 100.")
        @Digits(integer = 3, fraction = 2, message = "O percentual mínimo de frequência deve ter no máximo duas casas decimais.")
        BigDecimal minimumAttendancePercentage,

        @NotNull(message = "A lista de horários é obrigatória.")
        @Size(min = 1, max = 50, message = "A disciplina deve ter entre 1 e 50 horários.")
        List<@NotNull @Valid ClassScheduleRequest> schedules,

        @NotBlank(message = "O semestre é obrigatório.")
        @Pattern(regexp = "^[12]$", message = "O semestre deve ser 1 ou 2.")
        String semester,

        @NotBlank(message = "O período é obrigatório.")
        @Pattern(regexp = "^(19[0-9]{2}|20[0-9]{2}|21[0-9]{2}|2200)$", message = "O ano deve estar entre 1900 e 2200.")
        String periodo
) {
    @AssertTrue(message = "Os horários da disciplina não podem se sobrepor no mesmo dia.")
    public boolean isSchedulesNonOverlapping() {
        return DisciplineScheduleValidation.nonOverlapping(schedules);
    }
}
