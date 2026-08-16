package studdy.example.demo.grade;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import studdy.example.demo.grade.dto.CreateGradeRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateGradeRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsScoresAboveTen() {
        CreateGradeRequest request = new CreateGradeRequest(
                "Prova 1",
                new BigDecimal("10.01"),
                LocalDate.now()
        );

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage().equals("A nota não pode ser maior que 10.")));
    }

    @Test
    void acceptsTheMaximumScoreOfTen() {
        CreateGradeRequest request = new CreateGradeRequest(
                "Prova 1",
                new BigDecimal("10.00"),
                LocalDate.now()
        );

        assertTrue(validator.validate(request).isEmpty());
    }
}
