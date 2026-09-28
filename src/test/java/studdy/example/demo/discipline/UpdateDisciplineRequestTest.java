package studdy.example.demo.discipline;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import studdy.example.demo.discipline.dto.ClassScheduleRequest;
import studdy.example.demo.discipline.dto.UpdateDisciplineRequest;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateDisciplineRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsLegacyPeriodAndOverlappingSchedules() {
        UpdateDisciplineRequest request = new UpdateDisciplineRequest(
                "Cálculo", "Ana", "#4F46E5", new BigDecimal("6.00"), new BigDecimal("75.00"),
                List.of(
                        new ClassScheduleRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                        new ClassScheduleRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0))
                ), "2026.2", "2"
        );

        var messages = validator.validate(request).stream()
                .map(violation -> violation.getMessage()).toList();
        assertTrue(messages.contains("O semestre deve ser 1 ou 2."));
        assertTrue(messages.contains("O ano deve estar entre 1900 e 2200."));
        assertTrue(messages.contains("Os horários da disciplina não podem se sobrepor no mesmo dia."));
    }
}
