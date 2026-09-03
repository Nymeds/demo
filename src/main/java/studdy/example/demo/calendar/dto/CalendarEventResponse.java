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
        boolean disciplineDeleted,
        String title,
        String description,
        CalendarEventCategory category,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Instant createdAt,
        Instant updatedAt
) {

    private static final String DELETED_DISCIPLINE_NAME = "Essa disciplina não existe mais.";

    public static CalendarEventResponse from(CalendarEvent event) {
        Discipline discipline = event.getDiscipline();

        // Disciplina apagada: o banco zerou o vínculo, mas o nome copiado no momento da
        // criação continua ali provando que este evento um dia teve disciplina.
        boolean disciplineDeleted = discipline == null && event.getLinkedDisciplineName() != null;

        return new CalendarEventResponse(
                event.getId(),
                event.getDashboard().getId(),
                discipline == null ? null : discipline.getId(),
                disciplineName(discipline, disciplineDeleted),
                disciplineDeleted,
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private static String disciplineName(Discipline discipline, boolean disciplineDeleted) {
        if (discipline != null) {
            return discipline.getName();
        }

        return disciplineDeleted ? DELETED_DISCIPLINE_NAME : null;
    }
}
