package studdy.example.demo.discipline;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import studdy.example.demo.discipline.dto.UpdateDisciplineStatusRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateDisciplineStatusRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsAValidLifecycleStatus() {
        UpdateDisciplineStatusRequest request = new UpdateDisciplineStatusRequest(
                DisciplineLifecycleStatus.COMPLETED
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void requiresTheLifecycleStatus() {
        UpdateDisciplineStatusRequest request = new UpdateDisciplineStatusRequest(null);

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage().equals("A situação da disciplina é obrigatória.")));
    }
}
