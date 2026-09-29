package studdy.example.demo.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import studdy.example.demo.auth.dto.RegisterRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static final String TOO_LONG = "A senha é longa demais. Letras acentuadas e símbolos ocupam mais espaço; use menos caracteres.";

    private RegisterRequest withPassword(String password) {
        return new RegisterRequest("Estudante", "estudante@example.com", password, true, "2026-09-29");
    }

    @Test
    void rejectsAPasswordThatFitsInCharactersButNotInBcryptBytes() {
        // 40 caracteres, 80 bytes; 19 emojis = 38 caracteres UTF-16, 76 bytes.
        for (String password : new String[] {"á".repeat(40), "😀".repeat(19)}) {
            assertTrue(validator.validate(withPassword(password)).stream()
                    .anyMatch(violation -> violation.getMessage().equals(TOO_LONG)), password);
        }
    }

    @Test
    void acceptsAMultibytePasswordOfExactlySeventyTwoBytes() {
        assertTrue(validator.validate(withPassword("á".repeat(36))).isEmpty());
    }

    @Test
    void rejectsAnEmailLongerThanTheDatabaseColumn() {
        RegisterRequest request = new RegisterRequest(
                "Estudante",
                "a".repeat(140) + "@example.com",
                "senha-segura",
                true,
                "2026-09-29"
        );

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage()
                        .equals("O email deve ter no máximo 150 caracteres.")));
    }
}
