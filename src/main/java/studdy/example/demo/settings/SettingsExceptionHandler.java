package studdy.example.demo.settings;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

// Devolve o motivo do erro no campo "detail", que é o que a tela de configurações mostra ao
// estudante (por exemplo, "A senha atual está incorreta."). Restrito a este controller para
// não alterar as respostas das outras funcionalidades.
@RestControllerAdvice(assignableTypes = SettingsController.class)
class SettingsExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail handleStatus(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
    }
}
