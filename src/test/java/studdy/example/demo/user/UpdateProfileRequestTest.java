package studdy.example.demo.user;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import studdy.example.demo.user.dto.UpdateProfileRequest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateProfileRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsACompleteValidProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Gabriel Silva",
                "gabriel@example.com",
                "gabrielsilva",
                "(62) 99999-9999",
                LocalDate.of(2005, 3, 18),
                Gender.PREFER_NOT_TO_SAY,
                "Goiânia - GO"
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsEmptyOptionalFields() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Gabriel Silva",
                "gabriel@example.com",
                "",
                "",
                null,
                null,
                ""
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsBlankName() {
        assertRejects(validRequest("   ", "gabriel@example.com", "gabrielsilva", "(62) 99999-9999", null),
                "O nome é obrigatório.");
    }

    @Test
    void rejectsInvalidEmail() {
        assertRejects(validRequest("Gabriel", "email-inválido", "gabrielsilva", "(62) 99999-9999", null),
                "Informe um e-mail válido.");
    }

    @Test
    void rejectsInvalidUsername() {
        assertRejects(validRequest("Gabriel", "gabriel@example.com", "ga brie!", "(62) 99999-9999", null),
                "O nome de usuário deve ter entre 3 e 30 caracteres e usar apenas letras, números, ponto ou sublinhado.");
    }

    @Test
    void rejectsInvalidPhone() {
        assertRejects(validRequest("Gabriel", "gabriel@example.com", "gabrielsilva", "123", null),
                "Informe um telefone válido com pelo menos 8 números.");
    }

    @Test
    void rejectsFutureBirthDate() {
        assertRejects(validRequest(
                        "Gabriel",
                        "gabriel@example.com",
                        "gabrielsilva",
                        "(62) 99999-9999",
                        LocalDate.now().plusDays(1)
                ),
                "A data de nascimento deve estar no passado.");
    }

    private UpdateProfileRequest validRequest(
            String name,
            String email,
            String username,
            String phone,
            LocalDate birthDate
    ) {
        return new UpdateProfileRequest(
                name,
                email,
                username,
                phone,
                birthDate,
                Gender.PREFER_NOT_TO_SAY,
                "Goiânia - GO"
        );
    }

    private void assertRejects(UpdateProfileRequest request, String expectedMessage) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage().equals(expectedMessage)));
    }
}
