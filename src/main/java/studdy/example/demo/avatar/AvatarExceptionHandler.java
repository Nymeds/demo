package studdy.example.demo.avatar;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

// Devolve o motivo do erro no campo "detail", que a tela de Configurações mostra ao estudante.
// Restrito a este controller para não alterar as respostas das outras funcionalidades. O arquivo
// é lido só quando o controller pede (spring.servlet.multipart.resolve-lazily=true), por isso o
// estouro do limite de upload também chega aqui.
@RestControllerAdvice(assignableTypes = AvatarController.class)
class AvatarExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail handleStatus(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ProblemDetail handleMissingFile(MissingServletRequestPartException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, AvatarImageProcessor.EMPTY_UPLOAD_MESSAGE);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail handleTooLarge(MaxUploadSizeExceededException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, AvatarImageProcessor.TOO_LARGE_MESSAGE);
    }
}
