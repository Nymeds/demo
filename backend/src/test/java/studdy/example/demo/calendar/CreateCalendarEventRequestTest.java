package studdy.example.demo.calendar;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import studdy.example.demo.calendar.dto.CreateCalendarEventRequest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateCalendarEventRequestTest {

    private static final LocalDateTime MONDAY = LocalDateTime.of(2026, 9, 14, 8, 0);

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsACompleteEvent() {
        CreateCalendarEventRequest request = new CreateCalendarEventRequest(
                "Aula de Cálculo",
                "Limites e derivadas",
                CalendarEventCategory.CLASS,
                MONDAY,
                MONDAY.plusHours(2),
                null
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsAnEventWithoutAnEnd() {
        // Prazo de entrega não tem duração, só o instante em que vence.
        CreateCalendarEventRequest request = new CreateCalendarEventRequest(
                "Entregar trabalho",
                null,
                CalendarEventCategory.ASSIGNMENT,
                MONDAY.plusHours(15),
                null,
                null
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsABlankTitle() {
        CreateCalendarEventRequest request = new CreateCalendarEventRequest(
                "   ",
                null,
                CalendarEventCategory.CLASS,
                MONDAY,
                null,
                null
        );

        assertMessage(request, "O título do evento é obrigatório.");
    }

    @Test
    void rejectsATitleLongerThanTheColumn() {
        CreateCalendarEventRequest request = new CreateCalendarEventRequest(
                "a".repeat(121),
                null,
                CalendarEventCategory.CLASS,
                MONDAY,
                null,
                null
        );

        assertMessage(request, "O título do evento deve ter no máximo 120 caracteres.");
    }

    @Test
    void rejectsADescriptionLongerThanTheColumn() {
        CreateCalendarEventRequest request = new CreateCalendarEventRequest(
                "Prova 1",
                "a".repeat(501),
                CalendarEventCategory.EXAM,
                MONDAY,
                null,
                null
        );

        assertMessage(request, "A descrição deve ter no máximo 500 caracteres.");
    }

    @Test
    void rejectsAMissingCategory() {
        CreateCalendarEventRequest request = new CreateCalendarEventRequest(
                "Prova 1",
                null,
                null,
                MONDAY,
                null,
                null
        );

        assertMessage(request, "A categoria do evento é obrigatória.");
    }

    @Test
    void rejectsAMissingStart() {
        CreateCalendarEventRequest request = new CreateCalendarEventRequest(
                "Prova 1",
                null,
                CalendarEventCategory.EXAM,
                null,
                null,
                null
        );

        assertMessage(request, "A data e hora de início são obrigatórias.");
    }

    @Test
    void rejectsAnEndBeforeTheStart() {
        CreateCalendarEventRequest request = new CreateCalendarEventRequest(
                "Aula de Cálculo",
                null,
                CalendarEventCategory.CLASS,
                MONDAY,
                MONDAY.minusHours(1),
                null
        );

        assertMessage(request, "A data e hora de término devem ser posteriores às de início.");
    }

    @Test
    void rejectsAnEndEqualToTheStart() {
        CreateCalendarEventRequest request = new CreateCalendarEventRequest(
                "Aula de Cálculo",
                null,
                CalendarEventCategory.CLASS,
                MONDAY,
                MONDAY,
                null
        );

        assertMessage(request, "A data e hora de término devem ser posteriores às de início.");
    }

    private void assertMessage(CreateCalendarEventRequest request, String message) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage().equals(message)));
    }
}
