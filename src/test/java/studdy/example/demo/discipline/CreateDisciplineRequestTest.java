package studdy.example.demo.discipline;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import studdy.example.demo.discipline.dto.ClassScheduleRequest;
import studdy.example.demo.discipline.dto.CreateDisciplineRequest;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateDisciplineRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsAFullyFilledRequest() {
        assertTrue(validator.validate(newRequest(new BigDecimal("6.00"))).isEmpty());
    }

    @Test
    void requiresThePassingAverage() {
        assertRejects(newRequest(null), "A média de aprovação é obrigatória.");
    }

    @Test
    void rejectsAPassingAverageAboveTen() {
        assertRejects(newRequest(new BigDecimal("10.01")), "A média de aprovação não pode ser maior que 10.");
    }

    @Test
    void rejectsANegativePassingAverage() {
        assertRejects(newRequest(new BigDecimal("-1.00")), "A média de aprovação não pode ser negativa.");
    }

    @Test
    void rejectsAPassingAverageWithMoreThanTwoDecimalPlaces() {
        assertRejects(
                newRequest(new BigDecimal("6.005")),
                "A média de aprovação deve ter no máximo duas casas decimais."
        );
    }

    @Test
    void rejectsADisciplineWithoutSchedules() {
        CreateDisciplineRequest request = new CreateDisciplineRequest(
                "Cálculo",
                "Professora Ana",
                60,
                new BigDecimal("6.00"),
                List.of()
        );

        assertRejects(request, "A disciplina deve ter pelo menos um horário.");
    }

    @Test
    void rejectsAScheduleThatEndsBeforeItStarts() {
        CreateDisciplineRequest request = new CreateDisciplineRequest(
                "Cálculo",
                "Professora Ana",
                60,
                new BigDecimal("6.00"),
                List.of(new ClassScheduleRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(8, 0)))
        );

        assertRejects(request, "O horário final deve ser posterior ao horário inicial.");
    }

    @Test
    void rejectsANonPositiveWorkload() {
        CreateDisciplineRequest request = new CreateDisciplineRequest(
                "Cálculo",
                "Professora Ana",
                0,
                new BigDecimal("6.00"),
                List.of(new ClassScheduleRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)))
        );

        assertRejects(request, "A carga horária deve ser maior que zero.");
    }

    private CreateDisciplineRequest newRequest(BigDecimal passingAverage) {
        return new CreateDisciplineRequest(
                "Cálculo",
                "Professora Ana",
                60,
                passingAverage,
                List.of(new ClassScheduleRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)))
        );
    }

    private void assertRejects(CreateDisciplineRequest request, String expectedMessage) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage().equals(expectedMessage)));
    }
}
