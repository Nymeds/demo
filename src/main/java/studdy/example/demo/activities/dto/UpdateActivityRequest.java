package studdy.example.demo.activities.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import studdy.example.demo.activities.ActivityStatus;

import java.time.LocalDate;

public record UpdateActivityRequest(

        @NotBlank(message = "O título da atividade é obrigatório.")
        @Size(max = 160, message = "O título deve ter no máximo 160 caracteres.")
        String title,

        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres.")
        String description,

        @NotNull(message = "A data de entrega é obrigatória.")
        LocalDate dueDate,

        @NotNull(message = "O status da atividade é obrigatório.")
        ActivityStatus status

) {
}