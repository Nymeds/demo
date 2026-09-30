package studdy.example.demo.discipline;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import studdy.example.demo.discipline.dto.CreateAbsenceRecordRequest;

class CreateAbsenceRecordRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsACompleteAbsenceRecord() {
        assertTrue(validator.validate(new CreateAbsenceRecordRequest(
                LocalDate.now(), 1, "Saúde", "Consulta médica"
        )).isEmpty());
    }

    @Test
    void rejectsMissingOrInvalidRequiredFields() {
        assertTrue(validator.validate(new CreateAbsenceRecordRequest(null, 0, "", "")).size() >= 3);
    }

    @Test
    void limitsTheOptionalNoteToThreeHundredCharacters() {
        assertTrue(validator.validate(new CreateAbsenceRecordRequest(
                LocalDate.now(), 1, "Outro", "x".repeat(301)
        )).stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("note")));
    }
}
