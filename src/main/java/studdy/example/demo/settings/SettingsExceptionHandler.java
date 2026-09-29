package studdy.example.demo.settings;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.config.ValidationExceptionHandler;
import studdy.example.demo.user.UserController;

// Devolve o motivo do erro no campo "detail", que é o que a tela de configurações mostra ao
// estudante (por exemplo, "A senha atual está incorreta."). Restrito a este controller para
// não alterar as respostas das outras funcionalidades.
@RestControllerAdvice(assignableTypes = {SettingsController.class, UserController.class})
class SettingsExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail handleStatus(ResponseStatusException exception) {
        return ValidationExceptionHandler.problemFor(exception);
    }

    // O arquivo é lido só quando o controller pede (resolve-lazily), então o estouro do limite de
    // upload da foto de perfil também chega aqui.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail handleTooLarge(MaxUploadSizeExceededException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, "A foto de perfil deve ter no máximo 2 MB.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ProblemDetail handleMissingFile(MissingServletRequestPartException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Selecione uma foto de perfil.");
    }
}
