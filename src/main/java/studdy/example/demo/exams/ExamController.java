package studdy.example.demo.exams;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import studdy.example.demo.exams.dto.CreateExamRequest;
import studdy.example.demo.exams.dto.ExamResponse;
import studdy.example.demo.exams.dto.ExamSummaryResponse;
import studdy.example.demo.exams.dto.UpdateExamRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
    "/api/v1/dashboards/{dashboardId}/exams"
)
public class ExamController {

    private final ExamService examService;

    public ExamController(
        ExamService examService
    ) {

        this.examService = examService;
    }

    // CADASTRAR NOVA PROVA

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResponse create(

        @AuthenticationPrincipal UUID userId,

        @PathVariable UUID dashboardId,

        @Valid
        @RequestBody CreateExamRequest request

    ) {

        return examService.create(

            userId,

            dashboardId,

            request
        );
    }

    // LISTAR E FILTRAR PROVAS

    @GetMapping
    public Page<ExamResponse> findAll(

        @AuthenticationPrincipal UUID userId,

        @PathVariable UUID dashboardId,

        @RequestParam(required = false)
        String search,

        @RequestParam(required = false)
        UUID disciplineId,

        @RequestParam(required = false)
        ExamStatus status,

        @RequestParam(required = false)
        String period,

        @RequestParam(defaultValue = "0")
        int page,

        @RequestParam(defaultValue = "6")
        int size

    ) {

        return examService.findAll(

            userId,

            dashboardId,

            search,

            disciplineId,

            status,

            period,

            page,

            size
        );
    }

    // RESUMO DOS QUATRO CARTÕES

    @GetMapping("/summary")
    public ExamSummaryResponse summary(

        @AuthenticationPrincipal UUID userId,

        @PathVariable UUID dashboardId

    ) {

        return examService.summary(

            userId,

            dashboardId
        );
    }

    // PRÓXIMAS PROVAS

    @GetMapping("/upcoming")
    public List<ExamResponse> upcoming(

        @AuthenticationPrincipal UUID userId,

        @PathVariable UUID dashboardId,

        @RequestParam(defaultValue = "3")
        int limit

    ) {

        return examService.upcoming(

            userId,

            dashboardId,

            limit
        );
    }

    // CALENDÁRIO DE PROVAS

    @GetMapping("/calendar")
    public List<ExamResponse> calendar(

        @AuthenticationPrincipal UUID userId,

        @PathVariable UUID dashboardId,

        @RequestParam
        int year,

        @RequestParam
        int month

    ) {

        return examService.calendar(

            userId,

            dashboardId,

            year,

            month
        );
    }

    // CONSULTAR UMA PROVA

    @GetMapping("/{examId}")
    public ExamResponse findById(

        @AuthenticationPrincipal UUID userId,

        @PathVariable UUID dashboardId,

        @PathVariable UUID examId

    ) {

        return examService.findById(

            userId,

            dashboardId,

            examId
        );
    }

    // EDITAR UMA PROVA

    @PutMapping("/{examId}")
    public ExamResponse update(

        @AuthenticationPrincipal UUID userId,

        @PathVariable UUID dashboardId,

        @PathVariable UUID examId,

        @Valid
        @RequestBody UpdateExamRequest request

    ) {

        return examService.update(

            userId,

            dashboardId,

            examId,

            request
        );
    }

    // EXCLUIR UMA PROVA

    @DeleteMapping("/{examId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(

        @AuthenticationPrincipal UUID userId,

        @PathVariable UUID dashboardId,

        @PathVariable UUID examId

    ) {

        examService.delete(

            userId,

            dashboardId,

            examId
        );
    }
}