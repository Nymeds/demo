package studdy.example.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.security.TooManyRequestsException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidationExceptionHandlerTest {

    @Test
    void usesAGenericPortugueseDetailWhenTheReasonIsMissing() {
        ProblemDetail problem = ValidationExceptionHandler.problemFor(new ResponseStatusException(HttpStatus.CONFLICT));

        assertEquals(409, problem.getStatus());
        assertEquals(ValidationExceptionHandler.GENERIC_DETAIL, problem.getDetail());
    }

    @Test
    void keepsTheReasonWhenPresent() {
        ProblemDetail problem = ValidationExceptionHandler.problemFor(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha atual está incorreta."));

        assertEquals("A senha atual está incorreta.", problem.getDetail());
    }

    @Test
    void responseStatusExceptionsKeepTheirReasonForEveryController() {
        ProblemDetail problem = new ValidationExceptionHandler().handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Disciplina não encontrada."));

        assertEquals(404, problem.getStatus());
        assertEquals("Disciplina não encontrada.", problem.getDetail());
    }

    @Test
    void tooManyRequestsHasRetryAfterAndMinutesInTheDetail() {
        ResponseEntity<ProblemDetail> response = new ValidationExceptionHandler()
                .handleTooManyRequests(new TooManyRequestsException("Muitas tentativas.", 61));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("61", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals("Muitas tentativas. Tente novamente em 2 minutos.", response.getBody().getDetail());
        assertEquals("Muitas tentativas", response.getBody().getTitle());
    }
}
