package studdy.example.demo.discipline.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import studdy.example.demo.discipline.AcademicPerformance;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineLifecycleStatus;
import studdy.example.demo.discipline.DisciplineStatus;

public record DisciplineResponse(
        UUID id,
        UUID dashboardId,
        String name,
        String professorName,
        String color,
        BigDecimal minimumAttendancePercentage,
        List<ClassScheduleResponse> schedules,
        BigDecimal average,
        BigDecimal passingAverage,
        BigDecimal attendancePercentage,
        DisciplineLifecycleStatus status,
        DisciplineStatus performanceStatus,
        Instant createdAt,
        Instant updatedAt,
        String semester,
        String periodo
) {

    public static DisciplineResponse from(Discipline discipline, AcademicPerformance performance) {
        List<ClassScheduleResponse> schedules = discipline.getSchedules().stream()
                .map(ClassScheduleResponse::from)
                .toList();

        return new DisciplineResponse(
                discipline.getId(),
                discipline.getDashboard().getId(),
                discipline.getName(),
                discipline.getProfessorName(),
                discipline.getColor(),
                discipline.getMinimumAttendancePercentage(),
                schedules,
                performance.average(),
                performance.passingAverage(),
                discipline.getFrequency() == null ? new BigDecimal("100.00") : discipline.getFrequency().attendancePercentage(),
                discipline.getStatus(),
                performance.status(),
                discipline.getCreatedAt(),
                discipline.getUpdatedAt()
                ,
                discipline.getSemester(),
                discipline.getPeriodo()
        );
    }
}
