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

    @Test
    void rejectsNegativeScores() {
        CreateGradeRequest request = new CreateGradeRequest(
                "Prova 1",
                new BigDecimal("-0.01"),
                LocalDate.now()
        );

        assertRejects(request, "A nota não pode ser negativa.");
    }

    @Test
    void rejectsScoresWithMoreThanTwoDecimalPlaces() {
        CreateGradeRequest request = new CreateGradeRequest(
                "Prova 1",
                new BigDecimal("7.555"),
                LocalDate.now()
        );

        assertRejects(request, "A nota deve ter no máximo duas casas decimais.");
    }

    @Test
    void rejectsABlankAssessmentName() {
        CreateGradeRequest request = new CreateGradeRequest(
                "   ",
                new BigDecimal("8.00"),
                LocalDate.now()
        );

        assertRejects(request, "O nome da avaliação é obrigatório.");
    }

    @Test
    void rejectsARecordedDateInTheFuture() {
        CreateGradeRequest request = new CreateGradeRequest(
                "Prova 1",
                new BigDecimal("8.00"),
                LocalDate.now().plusDays(1)
        );

        assertRejects(request, "A data do registro não pode estar no futuro.");
    }

    private void assertRejects(CreateGradeRequest request, String expectedMessage) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage().equals(expectedMessage)));
    }
}
