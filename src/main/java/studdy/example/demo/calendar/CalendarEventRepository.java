package studdy.example.demo.calendar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

    List<CalendarEvent> findAllByDashboard_IdAndStartsAtBetweenOrderByStartsAtAsc(
            UUID dashboardId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<CalendarEvent> findAllByDashboard_IdAndCategoryInAndStartsAtBetweenOrderByStartsAtAsc(
            UUID dashboardId,
            List<CalendarEventCategory> categories,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<CalendarEvent> findByIdAndDashboard_Id(UUID id, UUID dashboardId);
}
