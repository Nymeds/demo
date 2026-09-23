package studdy.example.demo.exams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import studdy.example.demo.exams.ExamStatus;
import studdy.example.demo.exams.ExamType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateExamRequest(

    @NotNull(
        message = "Selecione uma disciplina."
    )
    UUID disciplineId,

    @NotBlank(
        message = "O título da prova é obrigatório."
    )
    @Size(max = 160)
    String title,

    @NotNull(
        message = "O tipo da prova é obrigatório."
    )
    ExamType type,

    @NotNull(
        message = "A data da prova é obrigatória."
    )
    LocalDate date,

    @NotNull(
        message = "O horário da prova é obrigatório."
    )
    LocalTime startTime,

    @Size(max = 2000)
    String content,

    @NotBlank(
        message = "Informe o peso/etapa da prova."
    )
    @Size(max = 20)
    String weightLabel,

    ExamStatus status

) {}