package studdy.example.demo.calendar;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import studdy.example.demo.calendar.dto.UpdateCalendarEventRequest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

// O DTO de edição repete as regras do de criação; aqui só garanto que o PUT não
// afrouxou nenhuma delas por descuido.
class UpdateCalendarEventRequestTest {

    private static final LocalDateTime MONDAY = LocalDateTime.of(2026, 9, 14, 8, 0);

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsACompleteEvent() {
        UpdateCalendarEventRequest request = new UpdateCalendarEventRequest(
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
    void rejectsABlankTitle() {
        UpdateCalendarEventRequest request = new UpdateCalendarEventRequest(
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
    void rejectsAMissingCategory() {
        UpdateCalendarEventRequest request = new UpdateCalendarEventRequest(
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
    void rejectsAnEndBeforeTheStart() {
        UpdateCalendarEventRequest request = new UpdateCalendarEventRequest(
                "Aula de Cálculo",
                null,
                CalendarEventCategory.CLASS,
                MONDAY,
                MONDAY.minusHours(1),
                null
        );

        assertMessage(request, "A data e hora de término devem ser posteriores às de início.");
    }

    private void assertMessage(UpdateCalendarEventRequest request, String message) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage().equals(message)));
    }
}
