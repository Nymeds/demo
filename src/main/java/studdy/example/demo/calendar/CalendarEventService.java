package studdy.example.demo.calendar;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.calendar.dto.CalendarEventResponse;
import studdy.example.demo.calendar.dto.CreateCalendarEventRequest;
import studdy.example.demo.calendar.dto.UpdateCalendarEventRequest;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CalendarEventService {

    private static final int MAX_UPCOMING_LIMIT = 50;

    private final CalendarEventRepository calendarEventRepository;
    private final DashboardRepository dashboardRepository;
    private final DisciplineRepository disciplineRepository;

    public CalendarEventService(
            CalendarEventRepository calendarEventRepository,
            DashboardRepository dashboardRepository,
            DisciplineRepository disciplineRepository
    ) {
        this.calendarEventRepository = calendarEventRepository;
        this.dashboardRepository = dashboardRepository;
        this.disciplineRepository = disciplineRepository;
    }

    @Transactional
    public CalendarEventResponse create(
            UUID userId,
            UUID dashboardId,
            CreateCalendarEventRequest request
    ) {
        Dashboard dashboard = findOwnedDashboard(userId, dashboardId);

        CalendarEvent event = new CalendarEvent(
                dashboard,
                findLinkedDiscipline(dashboardId, request.disciplineId()),
                request.title().trim(),
                trimDescription(request.description()),
                request.category(),
                request.startsAt(),
                request.endsAt()
        );

        return CalendarEventResponse.from(calendarEventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> findByPeriod(
            UUID userId,
            UUID dashboardId,
            LocalDateTime start,
            LocalDateTime end,
            List<CalendarEventCategory> categories
    ) {
        findOwnedDashboard(userId, dashboardId);

        if (!end.isAfter(start)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A data final deve ser posterior à data inicial."
            );
        }

        List<CalendarEvent> events = categories == null || categories.isEmpty()
                ? calendarEventRepository.findAllInPeriod(dashboardId, start, end)
                : calendarEventRepository.findAllInPeriodByCategories(dashboardId, categories, start, end);

        return events.stream()
                .map(CalendarEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> findUpcoming(UUID userId, UUID dashboardId, int limit) {
        findOwnedDashboard(userId, dashboardId);

        if (limit < 1 || limit > MAX_UPCOMING_LIMIT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O limite deve estar entre 1 e " + MAX_UPCOMING_LIMIT + "."
            );
        }

        return calendarEventRepository
                .findAllByDashboard_IdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(
                        dashboardId,
                        LocalDateTime.now(),
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(CalendarEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CalendarEventResponse findById(UUID userId, UUID dashboardId, UUID eventId) {
        return CalendarEventResponse.from(findOwnedEvent(userId, dashboardId, eventId));
    }

    @Transactional
    public CalendarEventResponse update(
            UUID userId,
            UUID dashboardId,
            UUID eventId,
            UpdateCalendarEventRequest request
    ) {
        CalendarEvent event = findOwnedEvent(userId, dashboardId, eventId);

        event.update(
                findLinkedDiscipline(dashboardId, request.disciplineId()),
                request.title().trim(),
                trimDescription(request.description()),
                request.category(),
                request.startsAt(),
                request.endsAt()
        );

        return CalendarEventResponse.from(event);
    }

    @Transactional
    public void delete(UUID userId, UUID dashboardId, UUID eventId) {
        calendarEventRepository.delete(findOwnedEvent(userId, dashboardId, eventId));
    }

    private Dashboard findOwnedDashboard(UUID userId, UUID dashboardId) {
        return dashboardRepository.findByIdAndOwner_Id(dashboardId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Dashboard não encontrado."
                ));
    }

    private CalendarEvent findOwnedEvent(UUID userId, UUID dashboardId, UUID eventId) {
        findOwnedDashboard(userId, dashboardId);

        return calendarEventRepository.findByIdAndDashboard_Id(eventId, dashboardId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Evento não encontrado."
                ));
    }

    private Discipline findLinkedDiscipline(UUID dashboardId, UUID disciplineId) {
        if (disciplineId == null) {
            return null;
        }

        return disciplineRepository.findByIdAndDashboard_Id(disciplineId, dashboardId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Disciplina não encontrada."
                ));
    }

    private String trimDescription(String description) {
        return description == null ? null : description.trim();
    }
}
