package studdy.example.demo.exams;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineAccessService;

import studdy.example.demo.exams.dto.CreateExamRequest;
import studdy.example.demo.exams.dto.ExamResponse;
import studdy.example.demo.exams.dto.ExamSummaryResponse;
import studdy.example.demo.exams.dto.UpdateExamRequest;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ExamService {

    private final ExamRepository examRepository;

    private final DisciplineAccessService disciplineAccessService;

    public ExamService(
        ExamRepository examRepository,
        DisciplineAccessService disciplineAccessService
    ) {

        this.examRepository = examRepository;

        this.disciplineAccessService =
            disciplineAccessService;
    }

    // CADASTRAR PROVA

    @Transactional
    public ExamResponse create(
        UUID userId,
        UUID dashboardId,
        CreateExamRequest request
    ) {

        Discipline discipline =
            disciplineAccessService.findOwnedDiscipline(
                userId,
                dashboardId,
                request.disciplineId()
            );

        Exam exam = new Exam(

            request.title().trim(),

            request.type(),

            request.date(),

            request.startTime(),

            normalizeContent(request.content()),

            request.weightLabel().trim(),

            request.status() == null
                ? ExamStatus.SCHEDULED
                : request.status(),

            discipline
        );

        return ExamResponse.from(
            examRepository.save(exam)
        );
    }

    // LISTAR, PESQUISAR E FILTRAR PROVAS

    @Transactional(readOnly = true)
    public Page<ExamResponse> findAll(

        UUID userId,

        UUID dashboardId,

        String search,

        UUID disciplineId,

        ExamStatus status,

        String period,

        int page,

        int size

    ) {

        List<ExamResponse> matches =

            dashboardExams(userId, dashboardId)
                .stream()

                .filter(e ->
                    disciplineId == null
                    || e.getDiscipline()
                        .getId()
                        .equals(disciplineId)
                )

                .filter(e ->
                    status == null
                    || e.getStatus() == status
                )

                .filter(e ->
                    period == null
                    || period.isBlank()
                    || e.getDiscipline()
                        .getPeriodo()
                        .equalsIgnoreCase(period.trim())
                )

                .filter(e ->
                    matchesSearch(e, search)
                )

                .map(ExamResponse::from)

                .toList();

        int safePage = Math.max(page, 0);

        int safeSize = Math.max(
            1,
            Math.min(size, 100)
        );

        long from = (long) safePage * safeSize;

        List<ExamResponse> content =

            from >= matches.size()

                ? List.of()

                : matches.subList(

                    (int) from,

                    (int) Math.min(
                        from + safeSize,
                        matches.size()
                    )
                );

        return new PageImpl<>(

            content,

            PageRequest.of(
                safePage,
                safeSize
            ),

            matches.size()
        );
    }

    // CONSULTAR UMA PROVA

    @Transactional(readOnly = true)
    public ExamResponse findById(

        UUID userId,

        UUID dashboardId,

        UUID examId

    ) {

        return ExamResponse.from(

            findOwnedExam(
                userId,
                dashboardId,
                examId
            )
        );
    }

    // ATUALIZAR UMA PROVA

    @Transactional
    public ExamResponse update(

        UUID userId,

        UUID dashboardId,

        UUID examId,

        UpdateExamRequest request

    ) {

        Exam exam = findOwnedExam(

            userId,

            dashboardId,

            examId
        );

        Discipline discipline =
            disciplineAccessService.findOwnedDiscipline(

                userId,

                dashboardId,

                request.disciplineId()
            );

        exam.update(

            request.title().trim(),

            request.type(),

            request.date(),

            request.startTime(),

            normalizeContent(request.content()),

            request.weightLabel().trim(),

            request.status(),

            discipline
        );

        return ExamResponse.from(exam);
    }

    // EXCLUIR UMA PROVA

    @Transactional
    public void delete(

        UUID userId,

        UUID dashboardId,

        UUID examId

    ) {

        examRepository.delete(

            findOwnedExam(

                userId,

                dashboardId,

                examId
            )
        );
    }

    // INDICADORES DOS QUATRO CARTÕES

    @Transactional(readOnly = true)
    public ExamSummaryResponse summary(

        UUID userId,

        UUID dashboardId

    ) {

        List<Exam> exams =
            dashboardExams(
                userId,
                dashboardId
            );

        LocalDate today = LocalDate.now();

        long completed = exams.stream()

            .filter(e ->
                e.getStatus()
                    == ExamStatus.COMPLETED
            )

            .count();

        long upcoming = exams.stream()

            .filter(e ->
                isUpcoming(e, today)
            )

            .count();

        long thisMonth = exams.stream()

            .filter(e ->
                YearMonth.from(e.getDate())
                    .equals(
                        YearMonth.from(today)
                    )
            )

            .count();

        int completedPercentage =

            exams.isEmpty()

                ? 0

                : (int) Math.round(

                    completed * 100.0
                    / exams.size()
                );

        return new ExamSummaryResponse(

            exams.size(),

            upcoming,

            thisMonth,

            completed,

            completedPercentage
        );
    }

    // PRÓXIMAS PROVAS

    @Transactional(readOnly = true)
    public List<ExamResponse> upcoming(

        UUID userId,

        UUID dashboardId,

        int limit

    ) {

        LocalDate today = LocalDate.now();

        return dashboardExams(
            userId,
            dashboardId
        )

        .stream()

        .filter(e ->
            isUpcoming(e, today)
        )

        .limit(
            Math.max(
                1,
                Math.min(limit, 100)
            )
        )

        .map(ExamResponse::from)

        .toList();
    }

    // CALENDÁRIO DE PROVAS

    @Transactional(readOnly = true)
    public List<ExamResponse> calendar(

        UUID userId,

        UUID dashboardId,

        int year,

        int month

    ) {

        YearMonth selected =
            YearMonth.of(
                year,
                month
            );

        return dashboardExams(

            userId,

            dashboardId

        )

        .stream()

        .filter(e ->
            YearMonth.from(e.getDate())
                .equals(selected)
        )

        .map(ExamResponse::from)

        .toList();
    }

    // PROVAS DO DASHBOARD AUTENTICADO

    private List<Exam> dashboardExams(

        UUID userId,

        UUID dashboardId

    ) {

        disciplineAccessService
            .findOwnedDashboard(

                userId,

                dashboardId
            );

        return examRepository
            .findAllByDiscipline_Dashboard_IdOrderByDateAscStartTimeAsc(

                dashboardId
            );
    }

    // LOCALIZA UMA PROVA DO USUÁRIO

    private Exam findOwnedExam(

        UUID userId,

        UUID dashboardId,

        UUID examId

    ) {

        disciplineAccessService
            .findOwnedDashboard(

                userId,

                dashboardId
            );

        return examRepository
            .findByIdAndDiscipline_Dashboard_Id(

                examId,

                dashboardId
            )

            .orElseThrow(() ->

                new ResponseStatusException(

                    HttpStatus.NOT_FOUND,

                    "Prova não encontrada."
                )
            );
    }

    // VERIFICA SE A PROVA ESTÁ PRÓXIMA

    private boolean isUpcoming(

        Exam exam,

        LocalDate today

    ) {

        return exam.getStatus()
                == ExamStatus.SCHEDULED

            && !exam.getDate()
                .isBefore(today)

            && !exam.getDate()
                .isAfter(
                    today.plusDays(7)
                );
    }

    // PESQUISA POR TÍTULO, DISCIPLINA E CONTEÚDO

    private boolean matchesSearch(

        Exam exam,

        String search

    ) {

        if (
            search == null
            || search.isBlank()
        ) {

            return true;
        }

        String q = search
            .trim()
            .toLowerCase(Locale.ROOT);

        return exam.getTitle()
                .toLowerCase(Locale.ROOT)
                .contains(q)

            || exam.getDiscipline()
                .getName()
                .toLowerCase(Locale.ROOT)
                .contains(q)

            || (
                exam.getContent() != null

                && exam.getContent()
                    .toLowerCase(Locale.ROOT)
                    .contains(q)
            );
    }

    private String normalizeContent(

        String content

    ) {

        return content == null
                || content.isBlank()

            ? null

            : content.trim();
    }
}