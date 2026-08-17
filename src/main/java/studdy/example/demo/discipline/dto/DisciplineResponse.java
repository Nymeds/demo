package studdy.example.demo.discipline.dto;

import studdy.example.demo.discipline.AcademicPerformance;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DisciplineResponse(
        UUID id,
        UUID dashboardId,
        String name,
        String professorName,
        Integer workloadHours,
        List<ClassScheduleResponse> schedules,
        BigDecimal average,
        BigDecimal attendancePercentage,
        DisciplineStatus status,
        Instant createdAt,
        Instant updatedAt
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
                discipline.getWorkloadHours(),
                schedules,
                performance.average(),
                null,
                performance.status(),
                discipline.getCreatedAt(),
                discipline.getUpdatedAt()
        );
    }
}
