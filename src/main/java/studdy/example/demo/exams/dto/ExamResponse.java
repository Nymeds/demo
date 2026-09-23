package studdy.example.demo.exams.dto;

import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.exams.Exam;
import studdy.example.demo.exams.ExamStatus;
import studdy.example.demo.exams.ExamType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ExamResponse(

    UUID id,

    UUID disciplineId,

    String disciplineName,

    String period,

    String semester,

    String title,

    ExamType type,

    LocalDate date,

    LocalTime startTime,

    String content,

    String weightLabel,

    ExamStatus status,

    Instant createdAt,

    Instant updatedAt

) {

    public static ExamResponse from(Exam exam) {

        Discipline discipline = exam.getDiscipline();

        return new ExamResponse(

            exam.getId(),

            discipline.getId(),

            discipline.getName(),

            discipline.getPeriodo(),

            discipline.getSemester(),

            exam.getTitle(),

            exam.getType(),

            exam.getDate(),

            exam.getStartTime(),

            exam.getContent(),

            exam.getWeightLabel(),

            exam.getStatus(),

            exam.getCreatedAt(),

            exam.getUpdatedAt()

        );
    }
}