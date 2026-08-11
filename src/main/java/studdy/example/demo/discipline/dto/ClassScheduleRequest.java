package studdy.example.demo.discipline.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ClassScheduleRequest(
        @NotNull(message = "O dia da semana é obrigatório.")
        DayOfWeek dayOfWeek,

        @NotNull(message = "O horário inicial é obrigatório.")
        LocalTime startTime,

        @NotNull(message = "O horário final é obrigatório.")
        LocalTime endTime
) {

    @AssertTrue(message = "O horário final deve ser posterior ao horário inicial.")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
