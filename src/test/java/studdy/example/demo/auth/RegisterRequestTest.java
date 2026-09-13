package studdy.example.demo.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import studdy.example.demo.auth.dto.RegisterRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsAnEmailLongerThanTheDatabaseColumn() {
        RegisterRequest request = new RegisterRequest(
                "Estudante",
                "a".repeat(140) + "@example.com",
                "senha-segura"
        );

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage()
                        .equals("O email deve ter no máximo 150 caracteres.")));
    }
}
