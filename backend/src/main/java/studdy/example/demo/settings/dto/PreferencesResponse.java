package studdy.example.demo.settings.dto;

import studdy.example.demo.settings.StartSection;
import studdy.example.demo.settings.UserPreferences;

import java.math.BigDecimal;

public record PreferencesResponse(
        int deadlineAlertDays,
        int attendanceAlertMargin,
        StartSection startSection,
        BigDecimal gradeGoal
) {
    public static PreferencesResponse from(UserPreferences preferences) {
        return new PreferencesResponse(
                preferences.getDeadlineAlertDays(),
                preferences.getAttendanceAlertMargin(),
                preferences.getStartSection(),
                preferences.getGradeGoal()
        );
    }

    public static PreferencesResponse defaults() {
        return new PreferencesResponse(
                UserPreferences.DEFAULT_DEADLINE_ALERT_DAYS,
                UserPreferences.DEFAULT_ATTENDANCE_ALERT_MARGIN,
                UserPreferences.DEFAULT_START_SECTION,
                null
        );
    }
}
