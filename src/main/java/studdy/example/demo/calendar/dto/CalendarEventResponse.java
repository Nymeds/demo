package studdy.example.demo.calendar.dto;

import studdy.example.demo.calendar.CalendarEvent;
import studdy.example.demo.calendar.CalendarEventCategory;
import studdy.example.demo.discipline.Discipline;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record CalendarEventResponse(
        UUID id,
        UUID dashboardId,
        UUID disciplineId,
        String disciplineName,
        String title,
        String description,
        CalendarEventCategory category,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static CalendarEventResponse from(CalendarEvent event) {
        Discipline discipline = event.getDiscipline();

        return new CalendarEventResponse(
                event.getId(),
                event.getDashboard().getId(),
                discipline == null ? null : discipline.getId(),
                discipline == null ? null : discipline.getName(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
