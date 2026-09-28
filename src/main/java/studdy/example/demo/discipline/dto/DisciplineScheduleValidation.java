package studdy.example.demo.discipline.dto;

import java.util.List;

final class DisciplineScheduleValidation {

    private DisciplineScheduleValidation() {
    }

    static boolean nonOverlapping(List<ClassScheduleRequest> schedules) {
        if (schedules == null || schedules.size() > 50) {
            return true;
        }

        for (int first = 0; first < schedules.size(); first++) {
            ClassScheduleRequest current = schedules.get(first);
            if (current == null || current.dayOfWeek() == null
                    || current.startTime() == null || current.endTime() == null) {
                continue;
            }
            for (int second = first + 1; second < schedules.size(); second++) {
                ClassScheduleRequest other = schedules.get(second);
                if (other != null && current.dayOfWeek() == other.dayOfWeek()
                        && other.startTime() != null && other.endTime() != null
                        && current.startTime().isBefore(other.endTime())
                        && other.startTime().isBefore(current.endTime())) {
                    return false;
                }
            }
        }
        return true;
    }
}
