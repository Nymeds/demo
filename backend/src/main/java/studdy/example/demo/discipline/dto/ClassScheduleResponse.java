package studdy.example.demo.discipline.dto;

import studdy.example.demo.discipline.ClassSchedule;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ClassScheduleResponse(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {

    public static ClassScheduleResponse from(ClassSchedule schedule) {
        return new ClassScheduleResponse(
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime()
        );
    }
}
