package studdy.example.demo.calendar;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

    // As duas consultas do período são @Query em vez de query method derivado do nome porque
    // a regra é de sobreposição, não de "começa dentro": o evento entra se ainda não terminou
    // quando o período começa. O coalesce trata o evento sem fim (um prazo) como instante,
    // usando o próprio início como término — senão todo prazo antigo vazaria para o período.
    @Query("""
            select event from CalendarEvent event
            where event.dashboard.id = :dashboardId
              and event.startsAt <= :end
              and coalesce(event.endsAt, event.startsAt) >= :start
            order by event.startsAt asc
            """)
    List<CalendarEvent> findAllInPeriod(
            @Param("dashboardId") UUID dashboardId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select event from CalendarEvent event
            where event.dashboard.id = :dashboardId
              and event.category in :categories
              and event.startsAt <= :end
              and coalesce(event.endsAt, event.startsAt) >= :start
            order by event.startsAt asc
            """)
    List<CalendarEvent> findAllInPeriodByCategories(
            @Param("dashboardId") UUID dashboardId,
            @Param("categories") List<CalendarEventCategory> categories,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<CalendarEvent> findAllByDashboard_IdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(
            UUID dashboardId,
            LocalDateTime from,
            Pageable pageable
    );

    Optional<CalendarEvent> findByIdAndDashboard_Id(UUID id, UUID dashboardId);
}
