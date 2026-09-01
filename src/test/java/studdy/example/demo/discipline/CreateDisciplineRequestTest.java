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

    private static final BigDecimal MINIMUM_ATTENDANCE = new BigDecimal("75.0");

    @Test
    void acceptsAFullyFilledRequest() {
        assertTrue(validator.validate(newRequest(new BigDecimal("6.00"))).isEmpty());
    }

    @Test
    void requiresThePassingAverage() {
        assertRejects(newRequest((BigDecimal) null), "A média de aprovação é obrigatória.");
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
    void requiresTheMinimumAttendancePercentage() {
        CreateDisciplineRequest request = newRequest(new BigDecimal("6.00"), null, defaultSchedules());

        assertRejects(request, "O percentual mínimo de frequência é obrigatório.");
    }

    @Test
    void rejectsAMinimumAttendanceAboveOneHundred() {
        CreateDisciplineRequest request = newRequest(new BigDecimal("6.00"), new BigDecimal("100.1"), defaultSchedules());

        assertRejects(request, "O percentual mínimo de frequência não pode ser maior que 100.");
    }

    @Test
    void acceptsAMinimumAttendanceOfOneHundred() {
        // A frequência vai de 0 a 100 e a média de 0 a 10: são escalas diferentes,
        // e 100 é válido só para a frequência.
        CreateDisciplineRequest request = newRequest(new BigDecimal("6.00"), new BigDecimal("100.0"), defaultSchedules());

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsADisciplineWithoutSchedules() {
        CreateDisciplineRequest request = newRequest(new BigDecimal("6.00"), MINIMUM_ATTENDANCE, List.of());

        assertRejects(request, "A disciplina deve ter pelo menos um horário.");
    }

    @Test
    void rejectsAScheduleThatEndsBeforeItStarts() {
        CreateDisciplineRequest request = newRequest(
                new BigDecimal("6.00"),
                MINIMUM_ATTENDANCE,
                List.of(new ClassScheduleRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(8, 0)))
        );

        assertRejects(request, "O horário final deve ser posterior ao horário inicial.");
    }

    @Test
    void requiresTheColor() {
        assertRejects(newRequestWithColor(null), "A cor da disciplina é obrigatória.");
    }

    @Test
    void rejectsAColorThatIsNotHexadecimal() {
        assertRejects(newRequestWithColor("azul"), "A cor da disciplina deve estar no formato hexadecimal.");
    }

    @Test
    void rejectsAThreeDigitHexadecimalColor() {
        // O front costuma abreviar para #FFF; o contrato exige os seis dígitos.
        assertRejects(newRequestWithColor("#FFF"), "A cor da disciplina deve estar no formato hexadecimal.");
    }

    @Test
    void acceptsARequestWithoutTheProfessorName() {
        // O nome do professor deixou de ser obrigatório no cadastro.
        CreateDisciplineRequest request = new CreateDisciplineRequest(
                "Cálculo",
                null,
                "#4F46E5",
                new BigDecimal("6.00"),
                MINIMUM_ATTENDANCE,
                defaultSchedules()
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    private CreateDisciplineRequest newRequest(BigDecimal passingAverage) {
        return newRequest(passingAverage, MINIMUM_ATTENDANCE, defaultSchedules());
    }

    private CreateDisciplineRequest newRequestWithColor(String color) {
        return new CreateDisciplineRequest(
                "Cálculo",
                "Professora Ana",
                color,
                new BigDecimal("6.00"),
                MINIMUM_ATTENDANCE,
                defaultSchedules()
        );
    }

    private CreateDisciplineRequest newRequest(
            BigDecimal passingAverage,
            BigDecimal minimumAttendancePercentage,
            List<ClassScheduleRequest> schedules
    ) {
        return new CreateDisciplineRequest(
                "Cálculo",
                "Professora Ana",
                "#4F46E5",
                passingAverage,
                minimumAttendancePercentage,
                schedules
        );
    }

    private List<ClassScheduleRequest> defaultSchedules() {
        return List.of(new ClassScheduleRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)));
    }

    private void assertRejects(CreateDisciplineRequest request, String expectedMessage) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage().equals(expectedMessage)));
    }
}
