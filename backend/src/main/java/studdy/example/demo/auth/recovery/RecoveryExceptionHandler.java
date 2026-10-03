package studdy.example.demo.auth.recovery;

import org.springframework.http.CacheControl;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = PasswordRecoveryController.class)
public class RecoveryExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handle(ResponseStatusException exception) {
        var response = ResponseEntity.status(exception.getStatusCode()).cacheControl(CacheControl.noStore());
        if (exception.getStatusCode().value() == 429) response.header("Retry-After", "3600");
        return response.body(ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason()));
    }
}
