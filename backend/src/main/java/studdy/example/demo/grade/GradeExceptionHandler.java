package studdy.example.demo.grade;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

// Devolve o motivo do erro no campo "detail", que o modal do Simulador de Notas mostra ao estudante
// (por exemplo, "Essa avaliação já tem uma nota lançada."). Restrito a este controller para não
// alterar as respostas das outras funcionalidades.
@RestControllerAdvice(assignableTypes = GradeController.class)
class GradeExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail handleStatus(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
    }
}
