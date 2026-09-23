package studdy.example.demo.exams.dto;

public record ExamSummaryResponse(

    long totalExams,

    long upcomingExams,

    long examsThisMonth,

    long completedExams,

    int completedPercentage

) {}