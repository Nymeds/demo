package studdy.example.demo.calendar.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import studdy.example.demo.calendar.CalendarEventCategory;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateCalendarEventRequest(
        @NotBlank(message = "O título do evento é obrigatório.")
        @Size(max = 120, message = "O título do evento deve ter no máximo 120 caracteres.")
        String title,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String description,

        @NotNull(message = "A categoria do evento é obrigatória.")
        CalendarEventCategory category,

        @NotNull(message = "A data e hora de início são obrigatórias.")
        LocalDateTime startsAt,

        LocalDateTime endsAt,

        UUID disciplineId
) {

    @AssertTrue(message = "A data e hora de término devem ser posteriores às de início.")
    public boolean isPeriodValid() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }
}
