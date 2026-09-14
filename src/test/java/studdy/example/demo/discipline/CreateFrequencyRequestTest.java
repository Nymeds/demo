package studdy.example.demo.discipline;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import studdy.example.demo.discipline.dto.CreateFrequencyRequest;
import studdy.example.demo.discipline.dto.UpdateFrequencyRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateFrequencyRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsADisciplineWithoutAnyAbsence() {
        assertTrue(validator.validate(new CreateFrequencyRequest(0)).isEmpty());
    }

    @Test
    void acceptsAbsencesWithoutAClassTotalOrAnArtificialUpperLimit() {
        assertTrue(validator.validate(new CreateFrequencyRequest(21)).isEmpty());
        assertTrue(validator.validate(new UpdateFrequencyRequest(21)).isEmpty());
    }

    @Test
    void requiresTheNumberOfAbsences() {
        assertRejects(new CreateFrequencyRequest(null), "A quantidade de faltas é obrigatória.");
    }

    @Test
    void rejectsInvalidUpdates() {
        assertTrue(!validator.validate(new UpdateFrequencyRequest(null)).isEmpty());
        assertTrue(!validator.validate(new UpdateFrequencyRequest(-1)).isEmpty());
    }

    @Test
    void rejectsANegativeNumberOfAbsences() {
        assertRejects(new CreateFrequencyRequest(-1), "A quantidade de faltas não pode ser negativa.");
    }

    private void assertRejects(CreateFrequencyRequest request, String expectedMessage) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage().equals(expectedMessage)));
    }
}
