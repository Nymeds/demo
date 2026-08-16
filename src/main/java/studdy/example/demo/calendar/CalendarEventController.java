package studdy.example.demo.calendar;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import studdy.example.demo.calendar.dto.CalendarEventResponse;
import studdy.example.demo.calendar.dto.CreateCalendarEventRequest;
import studdy.example.demo.calendar.dto.UpdateCalendarEventRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboards/{dashboardId}/calendar/events")
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    public CalendarEventController(CalendarEventService calendarEventService) {
        this.calendarEventService = calendarEventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalendarEventResponse create(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @Valid @RequestBody CreateCalendarEventRequest request
    ) {
        return calendarEventService.create(userId, dashboardId, request);
    }

    @GetMapping
    public List<CalendarEventResponse> findByPeriod(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) List<CalendarEventCategory> categories
    ) {
        return calendarEventService.findByPeriod(userId, dashboardId, start, end, categories);
    }

    @GetMapping("/upcoming")
    public List<CalendarEventResponse> findUpcoming(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return calendarEventService.findUpcoming(userId, dashboardId, limit);
    }

    @GetMapping("/{eventId}")
    public CalendarEventResponse findById(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID eventId
    ) {
        return calendarEventService.findById(userId, dashboardId, eventId);
    }

    @PutMapping("/{eventId}")
    public CalendarEventResponse update(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateCalendarEventRequest request
    ) {
        return calendarEventService.update(userId, dashboardId, eventId, request);
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID dashboardId,
            @PathVariable UUID eventId
    ) {
        calendarEventService.delete(userId, dashboardId, eventId);
    }
}
