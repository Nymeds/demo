package studdy.example.demo.activities;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import studdy.example.demo.activities.dto.CreateActivityRequest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateActivityRequestTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsBlankTitle() {

        CreateActivityRequest request =
                new CreateActivityRequest(
                        "   ",
                        "Descrição",
                        LocalDate.of(2026, 9, 10),
                        ActivityStatus.PENDING
                );

        assertRejects(
                request,
                "O título da atividade é obrigatório."
        );
    }

    @Test
    void rejectsTitleLongerThan160Characters() {

        CreateActivityRequest request =
                new CreateActivityRequest(
                        "A".repeat(161),
                        "Descrição",
                        LocalDate.of(2026, 9, 10),
                        ActivityStatus.PENDING
                );

        assertRejects(
                request,
                "O título deve ter no máximo 160 caracteres."
        );
    }

    @Test
    void rejectsDescriptionLongerThan2000Characters() {

        CreateActivityRequest request =
                new CreateActivityRequest(
                        "Atividade",
                        "A".repeat(2001),
                        LocalDate.of(2026, 9, 10),
                        ActivityStatus.PENDING
                );

        assertRejects(
                request,
                "A descrição deve ter no máximo 2000 caracteres."
        );
    }

    @Test
    void rejectsNullDueDate() {

        CreateActivityRequest request =
                new CreateActivityRequest(
                        "Atividade",
                        "Descrição",
                        null,
                        ActivityStatus.PENDING
                );

        assertRejects(
                request,
                "A data de entrega é obrigatória."
        );
    }

    @Test
    void rejectsNullStatus() {

        CreateActivityRequest request =
                new CreateActivityRequest(
                        "Atividade",
                        "Descrição",
                        LocalDate.of(2026, 9, 10),
                        null
                );

        assertRejects(
                request,
                "O status da atividade é obrigatório."
        );
    }

    private void assertRejects(
            CreateActivityRequest request,
            String expectedMessage
    ) {

        assertTrue(
                validator.validate(request)
                        .stream()
                        .anyMatch(
                                violation ->
                                        violation
                                                .getMessage()
                                                .equals(expectedMessage)
                        )
        );
    }
}