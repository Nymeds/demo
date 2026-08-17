package studdy.example.demo.discipline;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import studdy.example.demo.discipline.dto.CreateFrequencyRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateFrequencyRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsADisciplineWithoutAnyAbsence() {
        assertTrue(validator.validate(new CreateFrequencyRequest(60, 0)).isEmpty());
    }

    @Test
    void requiresTheTotalNumberOfClasses() {
        assertRejects(new CreateFrequencyRequest(null, 0), "A quantidade total de aulas é obrigatória.");
    }

    @Test
    void requiresTheNumberOfAbsences() {
        assertRejects(new CreateFrequencyRequest(60, null), "A quantidade de faltas é obrigatória.");
    }

    @Test
    void rejectsADisciplineWithoutClasses() {
        assertRejects(new CreateFrequencyRequest(0, 0), "A quantidade total de aulas deve ser maior que zero.");
    }

    @Test
    void rejectsANegativeNumberOfAbsences() {
        assertRejects(new CreateFrequencyRequest(60, -1), "A quantidade de faltas não pode ser negativa.");
    }

    private void assertRejects(CreateFrequencyRequest request, String expectedMessage) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage().equals(expectedMessage)));
    }
}
