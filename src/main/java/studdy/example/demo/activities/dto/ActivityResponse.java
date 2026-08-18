package studdy.example.demo.activities.dto;

import studdy.example.demo.activities.Activity;
import studdy.example.demo.activities.ActivityStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ActivityResponse(

        UUID id,
        UUID disciplineId,
        String title,
        String description,
        LocalDate dueDate,
        ActivityStatus status,
        Instant createdAt,
        Instant updatedAt

) {

    public static ActivityResponse from(Activity activity) {

        return new ActivityResponse(
                activity.getId(),
                activity.getDiscipline().getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getDueDate(),
                activity.getStatus(),
                activity.getCreatedAt(),
                activity.getUpdatedAt()
        );
    }
}
